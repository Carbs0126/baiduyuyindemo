package cn.carbs.testvoicerecg;

import android.Manifest;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.speech.RecognitionListener;
import android.speech.SpeechRecognizer;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.speech.VoiceRecognitionService;

import java.util.ArrayList;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private static final int EVENT_ERROR = 11;

    //语音转文字
    private Button btn_listening;
    private TextView tv_status;

    //文字转语音
    private EditText et_content;
    private Button btn_start;
    private Button btn_pause;
    private Button btn_resume;
    private Button btn_stop;

    public static final int STATUS_None = 0;
    public static final int STATUS_WaitingReady = 2;
    public static final int STATUS_Ready = 3;
    public static final int STATUS_Speaking = 4;
    public static final int STATUS_Recognition = 5;
    private int status = STATUS_None;
    private SpeechRecognizer speechRecognizer;

    private SpeechUtils speechUtils;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        requestSomePermission();
        initRecg();
        initTTS();
        initViews();
    }

    private void requestSomePermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.INTERNET)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.RECORD_AUDIO,
                            Manifest.permission.INTERNET}, 123);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 123
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "权限请求成功",Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.btn_listening:
                pressListening();
                break;
            case R.id.btn_start:
                startTTS();
                break;
            case R.id.btn_pause:
                pauseTTS();
                break;
            case R.id.btn_resume:
                resumeTTS();
                break;
            case R.id.btn_stop:
                stopTTS();
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        speechUtils.stop();
        speechUtils.release();
    }

    private void pressListening(){
        switch (status) {
            case STATUS_None:
                startRecg();
                btn_listening.setText("取消");
                status = STATUS_WaitingReady;
                break;
            case STATUS_WaitingReady:
                cancelRecg();
                status = STATUS_None;
                btn_listening.setText("开始听取语音");
                break;
            case STATUS_Ready:
                cancelRecg();
                status = STATUS_None;
                btn_listening.setText("开始听取语音");
                break;
            case STATUS_Speaking:
                stopRecg();
                status = STATUS_Recognition;
                btn_listening.setText("识别中");
                break;
            case STATUS_Recognition:
                cancelRecg();
                status = STATUS_None;
                btn_listening.setText("开始听取语音");
                break;
        }
    }

    private void startRecg(){
        Intent intent = new Intent();
        bindParams(intent);
        speechRecognizer.startListening(intent);
    }

    private void stopRecg(){
        speechRecognizer.stopListening();
//        print("点击了“说完了”");
    }

    private void cancelRecg(){
        speechRecognizer.cancel();
        status = STATUS_None;
//        print("点击了“取消”");
    }

    private void startTTS(){
        if (!TextUtils.isEmpty(et_content.getText().toString())) {
            speechUtils.speak(et_content.getText().toString());
        }
    }

    private void resumeTTS(){
        speechUtils.resume();
    }

    private void stopTTS(){
        speechUtils.stop();
    }

    private void pauseTTS(){
        speechUtils.pause();
    }

    private void initViews(){
        btn_listening = (Button) findViewById(R.id.btn_listening);
        btn_listening.setOnClickListener(this);
        tv_status = (TextView) findViewById(R.id.tv_status);

        et_content = (EditText) findViewById(R.id.et_content);
        btn_start = (Button) findViewById(R.id.btn_start);
        btn_start.setOnClickListener(this);
        btn_pause = (Button) findViewById(R.id.btn_pause);
        btn_pause.setOnClickListener(this);
        btn_resume = (Button) findViewById(R.id.btn_resume);
        btn_resume.setOnClickListener(this);
        btn_stop = (Button) findViewById(R.id.btn_stop);
        btn_stop.setOnClickListener(this);
    }

    private void initRecg(){
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this, new ComponentName(this, VoiceRecognitionService.class));

        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle bundle) {
                status = STATUS_Ready;
            }

            @Override
            public void onBeginningOfSpeech() {
                status = STATUS_Speaking;
            }

            @Override
            public void onRmsChanged(float v) {

            }

            @Override
            public void onBufferReceived(byte[] bytes) {

            }

            @Override
            public void onEndOfSpeech() {
                status = STATUS_Recognition;
            }

            @Override
            public void onError(int error) {
                status = STATUS_None;
                StringBuilder sb = new StringBuilder();
                switch (error) {
                    case SpeechRecognizer.ERROR_AUDIO:
                        sb.append("音频问题");
                        break;
                    case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                        sb.append("没有语音输入");
                        break;
                    case SpeechRecognizer.ERROR_CLIENT:
                        sb.append("其它客户端错误");
                        break;
                    case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                        sb.append("权限不足");
                        break;
                    case SpeechRecognizer.ERROR_NETWORK:
                        sb.append("网络问题");
                        break;
                    case SpeechRecognizer.ERROR_NO_MATCH:
                        sb.append("没有匹配的识别结果");
                        break;
                    case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                        sb.append("引擎忙");
                        break;
                    case SpeechRecognizer.ERROR_SERVER:
                        sb.append("服务端错误");
                        break;
                    case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                        sb.append("连接超时");
                        break;
                }
                sb.append(":" + error);
                Toast.makeText(MainActivity.this, "识别失败：" + sb.toString(), Toast.LENGTH_LONG).show();
                btn_listening.setText("开始听取语音");
            }

            @Override
            public void onResults(Bundle bundle) {
                status = STATUS_None;
                ArrayList<String> nbest = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                tv_status.setText("识别成功：" + Arrays.toString(nbest.toArray(new String[nbest.size()])));
            }

            @Override
            public void onPartialResults(Bundle bundle) {
                ArrayList<String> nbest = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (nbest.size() > 0) {
                    tv_status.setText("临时识别结果：" + Arrays.toString(nbest.toArray(new String[0])));
                }
            }

            @Override
            public void onEvent(int i, Bundle bundle) {
                switch (i) {
                    case EVENT_ERROR:
                        String reason = bundle.get("reason") + "";
                        tv_status.setText("EVENT_ERROR, " + reason);
                        break;
                    case VoiceRecognitionService.EVENT_ENGINE_SWITCH:
                        int type = bundle.getInt("engine_type");
                        tv_status.setText("*引擎切换至" + (type == 0 ? "在线" : "离线"));
                        break;
                }
            }
        });
    }

    private void initTTS(){
        speechUtils = SpeechUtils.getsSpeechUtils(this);
    }

    public void bindParams(Intent intent) {
        //录音是否有提示音
        {
            intent.putExtra(Constant.EXTRA_SOUND_START, R.raw.bdspeech_recognition_start);
            intent.putExtra(Constant.EXTRA_SOUND_END, R.raw.bdspeech_speech_end);
            intent.putExtra(Constant.EXTRA_SOUND_SUCCESS, R.raw.bdspeech_recognition_success);
            intent.putExtra(Constant.EXTRA_SOUND_ERROR, R.raw.bdspeech_recognition_error);
            intent.putExtra(Constant.EXTRA_SOUND_CANCEL, R.raw.bdspeech_recognition_cancel);
        }

        {
            //        infile	String	音频源	该参数支持设置为：
//        1. 文件系统路径，如："/sdcard/test/test.pcm"
//        2. java资源路径，如："res:///com/baidu.test/16k_test.pcm"
//        3. 数据源方法全名，格式如："#com.test.Factory.create8kInputStream()"（解释：Factory类中存在一个返回InputStream的方法create8kInputStream()）
//        注意：必须以#号开始；方法原型必须为：public static InputStream your_method()，而且该方法和类一定不能混淆，否则无法获取音频源。
//            String tmp = sp.getString(Constant.EXTRA_INFILE, "").replaceAll(",.*", "").trim();
//            intent.putExtra(Constant.EXTRA_INFILE, tmp);
        }

        {
            //        outfile	String	文件路径	保存识别过程产生的录音文件
            intent.putExtra(Constant.EXTRA_OUTFILE, "sdcard/outfile.pcm");
        }

        {
            //离线识别的语法路径（支持asset目录访问，如:assets:///mygram.bsg）
            intent.putExtra(Constant.EXTRA_GRAMMAR, "assets:///baidu_speech_grammar.bsg");
        }

        //采样率
//        if (sp.contains(Constant.EXTRA_SAMPLE)) {
//            String tmp = sp.getString(Constant.EXTRA_SAMPLE, "").replaceAll(",.*", "").trim();
//            if (null != tmp && !"".equals(tmp)) {
//                intent.putExtra(Constant.EXTRA_SAMPLE, Integer.parseInt(tmp));
//            }
//        }
//        if (sp.contains(Constant.EXTRA_LANGUAGE)) {
//            String tmp = sp.getString(Constant.EXTRA_LANGUAGE, "").replaceAll(",.*", "").trim();
//            if (null != tmp && !"".equals(tmp)) {
//                intent.putExtra(Constant.EXTRA_LANGUAGE, tmp);
//            }
//        }
//        if (sp.contains(Constant.EXTRA_NLU)) {
//            String tmp = sp.getString(Constant.EXTRA_NLU, "").replaceAll(",.*", "").trim();
//            if (null != tmp && !"".equals(tmp)) {
//                intent.putExtra(Constant.EXTRA_NLU, tmp);
//            }
//        }

        //语音活动检测
//        if (sp.contains(Constant.EXTRA_VAD)) {
//            String tmp = sp.getString(Constant.EXTRA_VAD, "").replaceAll(",.*", "").trim();
//            if (null != tmp && !"".equals(tmp)) {
//                intent.putExtra(Constant.EXTRA_VAD, tmp);
//            }
//        }
//        String prop = null;
//        if (sp.contains(Constant.EXTRA_PROP)) {
//            String tmp = sp.getString(Constant.EXTRA_PROP, "").replaceAll(",.*", "").trim();
//            if (null != tmp && !"".equals(tmp)) {
//                intent.putExtra(Constant.EXTRA_PROP, Integer.parseInt(tmp));
//                prop = tmp;
//            }
//        }

        // offline asr
//        {
//            intent.putExtra(Constant.EXTRA_OFFLINE_ASR_BASE_FILE_PATH, "/sdcard/easr/s_1");
//            if (null != prop) {
//                int propInt = Integer.parseInt(prop);
//                if (propInt == 10060) {
//                    intent.putExtra(Constant.EXTRA_OFFLINE_LM_RES_FILE_PATH, "/sdcard/easr/s_2_Navi");
//                } else if (propInt == 20000) {
//                    intent.putExtra(Constant.EXTRA_OFFLINE_LM_RES_FILE_PATH, "/sdcard/easr/s_2_InputMethod");
//                }
//            }
//            intent.putExtra(Constant.EXTRA_OFFLINE_SLOT_DATA, buildTestSlotData());
//        }
    }
}
