package com.semih.ilacim;

import android.app.*;
import android.content.*;
import android.hardware.Camera;
import android.os.*;
import android.view.*;
import android.widget.*;
import com.google.zxing.*;
import com.google.zxing.common.HybridBinarizer;
import java.util.*;
import java.util.concurrent.*;

@SuppressWarnings("deprecation")
public class ScannerActivity extends Activity implements SurfaceHolder.Callback {
  Camera camera;
  SurfaceView preview;
  boolean busy, finished;
  final ExecutorService worker = Executors.newSingleThreadExecutor();

  public void onCreate(Bundle b) {
    super.onCreate(b);
    LinearLayout root = new LinearLayout(this);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setBackgroundColor(0xff102b37);
    TextView title = new TextView(this);
    title.setText(
        "Barkodu veya karekodu kameraya göster\nKutunun üzerindeki kodu iyi ışıkta ortala.");
    title.setTextColor(-1);
    title.setTextSize(18);
    title.setPadding(24, 32, 24, 24);
    root.addView(title);
    preview = new SurfaceView(this);
    root.addView(preview, new LinearLayout.LayoutParams(-1, 0, 1));
    preview.getHolder().addCallback(this);
    Button close = new Button(this);
    close.setText(R.string.scanner_cancel);
    close.setOnClickListener(v -> finish());
    root.addView(close);
    setContentView(root);
  }

  public void surfaceCreated(SurfaceHolder h) {
    try {
      camera = Camera.open();
      camera.setDisplayOrientation(90);
      Camera.Parameters p = camera.getParameters();
      if (p.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE))
        p.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
      camera.setParameters(p);
      camera.setPreviewDisplay(h);
      camera.setPreviewCallback(
          (bytes, c) -> {
            if (busy || finished) return;
            busy = true;
            Camera.Size s = c.getParameters().getPreviewSize();
            byte[] copy = bytes.clone();
            worker.execute(
                () -> {
                  try {
                    MultiFormatReader reader = new MultiFormatReader();
                    Map<DecodeHintType, Object> hints = new EnumMap<>(DecodeHintType.class);
                    hints.put(
                        DecodeHintType.POSSIBLE_FORMATS,
                        Arrays.asList(
                            BarcodeFormat.EAN_13,
                            BarcodeFormat.EAN_8,
                            BarcodeFormat.UPC_A,
                            BarcodeFormat.CODE_128,
                            BarcodeFormat.DATA_MATRIX));
                    hints.put(DecodeHintType.TRY_HARDER, true);
                    reader.setHints(hints);
                    PlanarYUVLuminanceSource source =
                        new PlanarYUVLuminanceSource(
                            copy, s.width, s.height, 0, 0, s.width, s.height, false);
                    Result r;
                    try {
                      r = reader.decodeWithState(new BinaryBitmap(new HybridBinarizer(source)));
                    } catch (NotFoundException e) {
                      byte[] rotated = new byte[s.width * s.height];
                      for (int y = 0; y < s.height; y++)
                        for (int x = 0; x < s.width; x++)
                          rotated[x * s.height + s.height - y - 1] = copy[y * s.width + x];
                      source =
                          new PlanarYUVLuminanceSource(
                              rotated, s.height, s.width, 0, 0, s.height, s.width, false);
                      r = reader.decodeWithState(new BinaryBitmap(new HybridBinarizer(source)));
                    }
                    String code = Rules.barcode(r.getText());
                    runOnUiThread(
                        () -> {
                          if (!finished) {
                            finished = true;
                            setResult(RESULT_OK, new Intent().putExtra("barcode", code));
                            finish();
                          }
                        });
                  } catch (Exception ignored) {
                  } finally {
                    runOnUiThread(() -> busy = false);
                  }
                });
          });
      camera.startPreview();
    } catch (Exception e) {
      Toast.makeText(this, "Kamera açılamadı. Barkodu elle girebilirsin.", Toast.LENGTH_LONG)
          .show();
      finish();
    }
  }

  public void surfaceChanged(SurfaceHolder h, int f, int w, int height) {}

  public void surfaceDestroyed(SurfaceHolder h) {
    release();
  }

  void release() {
    if (camera != null) {
      camera.setPreviewCallback(null);
      camera.stopPreview();
      camera.release();
      camera = null;
    }
  }

  protected void onPause() {
    super.onPause();
    release();
  }

  protected void onDestroy() {
    finished = true;
    worker.shutdownNow();
    super.onDestroy();
  }
}
