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
    private static final ArrayList<ListenerSMS> listeners = new ArrayList<>();

    public static BroadcastReceiverSMS broadcastReceiverSMS;

    public static BroadcastReceiverSMS getInstance()
    {
        if(broadcastReceiverSMS == null)
            broadcastReceiverSMS = new BroadcastReceiverSMS();
        return broadcastReceiverSMS;
    }

    public BroadcastReceiverSMS()
    {
        broadcastReceiverSMS = this;
    }

    public boolean addListener(ListenerSMS listenerSMS)
    {
        synchronized (listeners)
        {
            for (ListenerSMS listener : listeners)
            {
                if (listener == listenerSMS)
                    return false;
            }
            listeners.add(listenerSMS);
            Log.d(LOG_TAG, "Listeners: " + listeners.size());
            return true;
        }
    }

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

                updateMessages(smsMessage);
                stringBuilder.append(String.format(Locale.US, "SMS %d From: %s \n%s\n", i, address, smsBody));
            }

            Log.d(LOG_TAG, stringBuilder.toString());
        }
    }

    public void updateMessages(SmsMessage message)
    {
        synchronized (messages)
        {
            messages.add(message);
        }
        synchronized (listeners)
        {
            for(ListenerSMS listener : listeners)
            {
                listener.onSMSReceived(message);
            }
        }
    }

    public ArrayList<SmsMessage> getPastMessages()
    {
        ArrayList<SmsMessage> messageCopies = new ArrayList<>();
        synchronized (messages)
        {
            messageCopies.addAll(messages);
        }
        return messageCopies;
    }

}
