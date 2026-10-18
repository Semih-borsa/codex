package com.semih.nova;

import android.content.Intent;
import android.net.Uri;

final class MessageIntents {
  static Intent build(MessageDraft draft) {
    draft.preview();
    String phone = MessageDraft.phone(draft.recipient, draft.whatsapp);
    if (draft.whatsapp)
      return new Intent(
              Intent.ACTION_VIEW,
              Uri.parse("https://wa.me/" + phone)
                  .buildUpon()
                  .appendQueryParameter("text", draft.body)
                  .build())
          .setPackage("com.whatsapp");
    return new Intent(Intent.ACTION_SENDTO, Uri.fromParts("smsto", phone, null))
        .putExtra("sms_body", draft.body);
  }
}
