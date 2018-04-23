package independent_study.commandcamera.sms;

import android.telephony.SmsMessage;

public interface ListenerSMS
{
    void onSMSReceived(SmsMessage message);
}
