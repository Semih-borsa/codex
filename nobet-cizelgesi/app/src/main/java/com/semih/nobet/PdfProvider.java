package com.semih.nobet;

import android.content.*;
import android.database.*;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import java.io.*;

/** Read-only, temporary URI grants; exposes only generated PDF files in one cache directory. */
public final class PdfProvider extends ContentProvider {
  public boolean onCreate() {
    return true;
  }

  private File file(Uri u) throws FileNotFoundException {
    String n = u.getLastPathSegment();
    if (n == null || !n.matches("(?:Nobet|Istatistik)_[0-9_]+\\.pdf"))
      throw new FileNotFoundException();
    File root = new File(getContext().getCacheDir(), "pdf");
    File f = new File(root, n);
    try {
      if (!f.getCanonicalFile().getParentFile().equals(root.getCanonicalFile()) || !f.isFile())
        throw new FileNotFoundException();
    } catch (IOException e) {
      throw new FileNotFoundException();
    }
    return f;
  }

  public String getType(Uri u) {
    return "application/pdf";
  }

  public ParcelFileDescriptor openFile(Uri u, String mode) throws FileNotFoundException {
    if (!"r".equals(mode)) throw new FileNotFoundException("Read only");
    return ParcelFileDescriptor.open(file(u), ParcelFileDescriptor.MODE_READ_ONLY);
  }

  public Cursor query(Uri u, String[] projection, String selection, String[] args, String sort) {
    try {
      File f = file(u);
      String[] cols =
          projection == null
              ? new String[] {OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE}
              : projection;
      MatrixCursor c = new MatrixCursor(cols);
      Object[] row = new Object[cols.length];
      for (int i = 0; i < cols.length; i++)
        row[i] =
            OpenableColumns.DISPLAY_NAME.equals(cols[i])
                ? f.getName()
                : OpenableColumns.SIZE.equals(cols[i]) ? f.length() : null;
      c.addRow(row);
      return c;
    } catch (FileNotFoundException e) {
      return null;
    }
  }

  public Uri insert(Uri u, ContentValues v) {
    throw new UnsupportedOperationException();
  }

  public int update(Uri u, ContentValues v, String a, String[] b) {
    throw new UnsupportedOperationException();
  }

  public int delete(Uri u, String a, String[] b) {
    throw new UnsupportedOperationException();
  }
}
