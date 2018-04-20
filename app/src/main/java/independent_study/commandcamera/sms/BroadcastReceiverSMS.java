package independent_study.commandcamera.sms;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;

import java.util.ArrayList;
import java.util.Locale;

/**
 * Class Meant to Receive SMS Messages for Activation
 */
public class BroadcastReceiverSMS extends BroadcastReceiver
{
    public static final String SMS_BUNDLE = "pdus";
    public static final String LOG_TAG = "BroadcastReceiverSMS";

    private static final ArrayList<SmsMessage> messages = new ArrayList<>();

    public void onReceive(Context context, Intent intent)
    {
        Bundle intentExtras = intent.getExtras();
        if (intentExtras != null)
        {
            Object[] sms = (Object[]) intentExtras.get(SMS_BUNDLE);
            StringBuilder stringBuilder = new StringBuilder();

            if(sms == null)
                return;

            for(int i = 0; i < sms.length; i++)
            {
                SmsMessage smsMessage = SmsMessage.createFromPdu((byte[]) sms[i]);

                String smsBody = smsMessage.getMessageBody();
                String address = smsMessage.getOriginatingAddress();

                messages.add(smsMessage);
                stringBuilder.append(String.format(Locale.US, "SMS From: %s \n%s\n", address, smsBody));
            }

            Log.d(LOG_TAG, stringBuilder.toString());
        }
    }
}
