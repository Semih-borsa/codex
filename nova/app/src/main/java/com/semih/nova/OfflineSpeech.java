package com.semih.nova;

import android.speech.tts.*;
import java.util.*;

final class OfflineSpeech {
  static Voice find(TextToSpeech tts) {
    Set<Voice> voices = tts.getVoices();
    if (voices != null)
      for (Voice v : voices)
        if ("tr".equals(v.getLocale().getLanguage())
            && !v.isNetworkConnectionRequired()
            && (v.getFeatures() == null
                || !v.getFeatures().contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED)))
          return v;
    return null;
  }
}
