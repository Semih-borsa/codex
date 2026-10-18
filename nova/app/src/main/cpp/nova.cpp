#include <jni.h>
#include <atomic>
#include <mutex>
#include <string>
#include <vector>
#include <memory>
#include <algorithm>
#include "llama.h"
#include "whisper.h"
static std::atomic<bool> stopped{false};
static std::mutex engineMutex;
static llama_model *model=nullptr;
static std::string activePath;
static whisper_context *voiceModel=nullptr;
static std::string voicePath;
static void fail(JNIEnv *e,const char *m){e->ThrowNew(e->FindClass("java/lang/IllegalStateException"),m);}
extern "C" JNIEXPORT void JNICALL Java_com_semih_nova_NativeBridge_reset(JNIEnv*,jclass){stopped=false;}
extern "C" JNIEXPORT void JNICALL Java_com_semih_nova_NativeBridge_stop(JNIEnv*,jclass){stopped=true;}
extern "C" JNIEXPORT jbyteArray JNICALL Java_com_semih_nova_NativeBridge_generate(JNIEnv *e,jclass,jstring path,jbyteArray input,jobject listener){
 std::lock_guard<std::mutex> guard(engineMutex);
 const char *p=e->GetStringUTFChars(path,nullptr);std::string filename(p);e->ReleaseStringUTFChars(path,p);
 if(!model||activePath!=filename){
  if(model){llama_model_free(model);model=nullptr;activePath.clear();}
  llama_backend_init();auto mp=llama_model_default_params();mp.n_gpu_layers=0;mp.use_mmap=true;
  model=llama_model_load_from_file(filename.c_str(),mp);
  if(!model){fail(e,"GGUF modeli açılamadı. Dosya eksik, bozuk veya bu motorla uyumsuz olabilir.");return nullptr;}
  activePath=filename;
 }
 auto cp=llama_context_default_params();cp.n_ctx=2048;cp.n_batch=256;cp.n_ubatch=128;cp.n_threads=4;cp.n_threads_batch=4;
 std::unique_ptr<llama_context,decltype(&llama_free)> ctx(llama_init_from_model(model,cp),llama_free);
 if(!ctx){fail(e,"Model için yeterli çalışma belleği ayrılamadı.");return nullptr;}
 int len=e->GetArrayLength(input);std::string prompt(len,' ');e->GetByteArrayRegion(input,0,len,reinterpret_cast<jbyte*>(prompt.data()));
 const auto *vocab=llama_model_get_vocab(model);
 int n=-llama_tokenize(vocab,prompt.data(),len,nullptr,0,true,true);
 if(n<=0||n>1750){fail(e,"Mesaj ve belge bağlamı çok uzun. Daha kısa bir soru veya belge bölümü seç.");return nullptr;}
 std::vector<llama_token> tokens(n);llama_tokenize(vocab,prompt.data(),len,tokens.data(),n,true,true);
 for(int i=0;i<n&&!stopped;i+=256){auto batch=llama_batch_get_one(tokens.data()+i,std::min(256,n-i));if(llama_decode(ctx.get(),batch)!=0){fail(e,"Model girdisi işlenemedi.");return nullptr;}}
 auto scp=llama_sampler_chain_default_params();
 std::unique_ptr<llama_sampler,decltype(&llama_sampler_free)> sampler(llama_sampler_chain_init(scp),llama_sampler_free);
 llama_sampler_chain_add(sampler.get(),llama_sampler_init_penalties(96,1.15f,.10f,0.0f));llama_sampler_chain_add(sampler.get(),llama_sampler_init_top_k(40));llama_sampler_chain_add(sampler.get(),llama_sampler_init_top_p(.9f,1));llama_sampler_chain_add(sampler.get(),llama_sampler_init_temp(.65f));llama_sampler_chain_add(sampler.get(),llama_sampler_init_dist(LLAMA_DEFAULT_SEED));
 jclass cls=e->GetObjectClass(listener);jmethodID method=e->GetMethodID(cls,"onBytes","([B)V");std::string output;
 for(int i=0;i<256&&!stopped;i++){
  llama_token token=llama_sampler_sample(sampler.get(),ctx.get(),-1);if(llama_vocab_is_eog(vocab,token))break;
  char buf[1024];int bytes=llama_token_to_piece(vocab,token,buf,sizeof(buf),0,false);
  if(bytes>0)output.append(buf,bytes);
  if(i%4==0){jbyteArray chunk=e->NewByteArray(output.size());e->SetByteArrayRegion(chunk,0,output.size(),reinterpret_cast<const jbyte*>(output.data()));e->CallVoidMethod(listener,method,chunk);e->DeleteLocalRef(chunk);if(e->ExceptionCheck())return nullptr;}
  auto batch=llama_batch_get_one(&token,1);if(llama_decode(ctx.get(),batch)!=0){fail(e,"Model cevabı işlenemedi.");return nullptr;}
 }
 jbyteArray result=e->NewByteArray(output.size());e->SetByteArrayRegion(result,0,output.size(),reinterpret_cast<const jbyte*>(output.data()));return result;
}
extern "C" JNIEXPORT void JNICALL Java_com_semih_nova_NativeBridge_release(JNIEnv*,jclass){std::lock_guard<std::mutex> guard(engineMutex);if(model){llama_model_free(model);model=nullptr;activePath.clear();}if(voiceModel){whisper_free(voiceModel);voiceModel=nullptr;voicePath.clear();}}

extern "C" JNIEXPORT jbyteArray JNICALL Java_com_semih_nova_NativeBridge_transcribe(JNIEnv *e,jclass,jstring path,jfloatArray audio){
 std::lock_guard<std::mutex> guard(engineMutex);
 const char *p=e->GetStringUTFChars(path,nullptr);std::string filename(p);e->ReleaseStringUTFChars(path,p);
 auto params=whisper_context_default_params();params.use_gpu=false;
 if(!voiceModel||voicePath!=filename){if(voiceModel)whisper_free(voiceModel);voiceModel=whisper_init_from_file_with_params(filename.c_str(),params);voicePath=filename;}
 auto *context=voiceModel;
 if(!context){fail(e,"Yerel ses modeli açılamadı.");return nullptr;}
 int count=e->GetArrayLength(audio);if(count<8000||count>480000){fail(e,"Ses kaydı 0,5–30 saniye olmalı.");return nullptr;}
 std::vector<float> samples(count);e->GetFloatArrayRegion(audio,0,count,samples.data());
 auto fp=whisper_full_default_params(WHISPER_SAMPLING_GREEDY);fp.language="tr";fp.translate=false;fp.n_threads=4;fp.no_context=true;fp.single_segment=true;fp.print_realtime=false;fp.print_progress=false;fp.print_timestamps=false;fp.suppress_blank=true;fp.abort_callback=[](void*){return stopped.load();};
 if(whisper_full(context,fp,samples.data(),count)!=0){fail(e,stopped?"Ses çözümleme durduruldu.":"Ses çözümlenemedi.");return nullptr;}
 std::string output;for(int i=0;i<whisper_full_n_segments(context);i++)output+=whisper_full_get_segment_text(context,i);
 jbyteArray result=e->NewByteArray(output.size());e->SetByteArrayRegion(result,0,output.size(),reinterpret_cast<const jbyte*>(output.data()));return result;
}
extern "C" JNIEXPORT jbyteArray JNICALL Java_com_semih_nova_NativeBridge_modelInfo(JNIEnv *e,jclass){
 std::lock_guard<std::mutex> guard(engineMutex);std::string text;
 if(model){char name[512]={0},arch[128]={0};llama_model_meta_val_str(model,"general.name",name,sizeof(name));llama_model_meta_val_str(model,"general.architecture",arch,sizeof(arch));text=std::string(name)+"\nMimari: "+arch;}
 jbyteArray out=e->NewByteArray(text.size());e->SetByteArrayRegion(out,0,text.size(),reinterpret_cast<const jbyte*>(text.data()));return out;
}
