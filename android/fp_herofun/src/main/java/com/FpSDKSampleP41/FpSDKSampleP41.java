package com.FpSDKSampleP41;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;

import com.HEROFUN.HostUsb;
import com.FpSDKSampleP41.R;
import com.HEROFUN.HAPI;
import com.HEROFUN.LAPI;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Bitmap.Config;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

public class FpSDKSampleP41 extends Activity {
    /** Called when the activity is first created. */
    //for LAPI
    private TextView tvLAPImsg;
    protected Button btnOpen;
    protected Button btnClose;
    private Button btnGetImage;
    private Button btnOnVideo;
    private Button btnCalibration;
    private Button btnGetImageQuality;
    private Button btnGetNFIQuality;
    private Button btnCreateAnsiTemp;
    private Button btnCreateIsoTemp;
    private Button btnCompareAnsiTemp;
    private Button btnCompareIsoTemp;
    private TextView tvANSITemp;
    private TextView tvISOTemp;
    private Button btnCompress;
    private Button btnUnCompress;
    private Button btnGetMake;
    private Button btnGetModel;
    private Button btnGetSN;

    private LAPI m_cLAPI = null;
    private HostUsb mHostUSb = null;

    private int m_hDevice = 0;
    private byte[] m_image = new byte[LAPI.WIDTH*LAPI.HEIGHT];
    private byte[] m_ansi_template = new byte[LAPI.FPINFO_STD_MAX_SIZE];
    private byte[] m_iso_template = new byte[LAPI.FPINFO_STD_MAX_SIZE];
    private byte[] bfwsq = new byte[512*512];
    private int[] RGBbits = new int[256 * 360];

    //for HAPI
    private TextView tvHAPImsg;
    private TextView txtRegID;
    private Button btnEnroll;
    private Button btnVerify;
    private Button btnSearch;
    private Button btnDBList;
    private Button btnDBClear;
    private Button btnDBDelete;
    private ImageView viewFinger;
    private ListView viewRecord;
    private RadioButton rdoANSI;
    private RadioButton rdoISO;
    private boolean option = true;		//ANSI-true, ISO-false
	private int sensor_mode = 0;
    private int store_cnt = 0;
	
    private String[] mListString = null;
    private String[] mFiletString = null;

    public static final int MESSAGE_SET_ID = 100;
    public static final int MESSAGE_SHOW_TEXT = 101;
    public static final int MESSAGE_VIEW_ANSI_TEMPLATE = 103;
    public static final int MESSAGE_VIEW_ISO_TEMPLATE = 104;
    public static final int MESSAGE_SHOW_IMAGE = 200;
    public static final int MESSAGE_ENABLE_BTN = 300;
    public static final int MESSAGE_SHOW_BITMAP = 303;
    public static final int MESSAGE_LIST_START = 400;
    public static final int MESSAGE_LIST_NEXT = 401;
    public static final int MESSAGE_LIST_END = 402;
    public static final int MESSAGE_ID_ENABLED = 403;
    public static final int MESSAGE_ID_SETTEXT= 404;

    private HAPI m_cHAPI = null;

    private boolean DEBUG = false;
    private volatile boolean bContinue = false;
    Activity myThis;

    private Context mContext;
    private ScreenBroadcastReceiver mScreenReceiver;
 	
    @Override
	protected void onResume() 
    {
        super.onResume();
    }
    @Override
	protected void onStart() 
    {
        super.onStart();
    }
    @Override
	protected void onPause() 
    {
		m_cHAPI.DoCancel();
		bContinue = false;
        super.onPause();
    }
    @Override
    public void onBackPressed()
    {
        super.onBackPressed();
    }
    @Override
	protected void onDestroy() 
    {
        if (m_hDevice != 0) CLOSE_DEVICE();
        super.onDestroy();
    }

    private void registerListener() {
        if (mContext != null) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_SCREEN_OFF);
            filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
            filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
            //filter.addAction(HostUsb.ACTION_USB_PERMISSION);
            mContext.registerReceiver(mScreenReceiver, filter);
        }
    }
 
    private class ScreenBroadcastReceiver extends BroadcastReceiver {
        private String action = null;
 
        @Override
        public void onReceive(Context context, Intent intent) {
            action = intent.getAction();
            if (Intent.ACTION_SCREEN_OFF.equals(action))
            {
                onDestroy();
            	finish();
            }
            else if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action))
            {
                UsbDevice newDevice = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (newDevice != null && isFingerDevice(newDevice)) {
                    m_cLAPI.setHostUsb(mHostUSb);
                     if(!mHostUSb.AuthorizeDevice(newDevice)){
                        Toast.makeText(context,"FingerDevice attached",Toast.LENGTH_LONG).show();
                    }
                }
            }
            else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action))
            {
                UsbDevice oldDevice = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (oldDevice != null && isFingerDevice(oldDevice)) {
                    Toast.makeText(context,"FingerDevice detached",Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    private boolean isFingerDevice(UsbDevice device){
        int vid = device.getVendorId();
        int pid = device.getProductId();
        if(vid == LAPI.VID && pid == LAPI.SCSI_PID){ //1155     22288
            return true;
        }

        return false;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        tvLAPImsg = (TextView) findViewById(R.id.LAPI_msg);
        btnOpen = (Button)findViewById(R.id.btnOpenDevice);
        btnClose = (Button)findViewById(R.id.btnCloseDevice);
        btnGetImage = (Button)findViewById(R.id.btnGetImage);
        btnOnVideo = (Button)findViewById(R.id.btnOnVideo);
        btnCalibration = (Button)findViewById(R.id.btnCalibration);
        btnGetImageQuality = (Button)findViewById(R.id.btnGetImageQuality);
        btnGetNFIQuality = (Button)findViewById(R.id.btnGetNFIQuality);
        btnCreateAnsiTemp = (Button)findViewById(R.id.btnCreateANSITemp);
        btnCreateIsoTemp = (Button)findViewById(R.id.btnCreateISOTemp);
        tvANSITemp = (TextView)findViewById(R.id.tvANSITemp);
        tvISOTemp = (TextView)findViewById(R.id.tvISOTemp);
        btnCompareAnsiTemp = (Button)findViewById(R.id.btnCompareANSITemp);
        btnCompareIsoTemp = (Button)findViewById(R.id.btnCompareISOTemp);
        btnCompress = (Button)findViewById(R.id.btnCompress);
        btnUnCompress = (Button)findViewById(R.id.btnUnCompress);
        btnGetMake = (Button)findViewById(R.id.btnGetMake);
        btnGetModel = (Button)findViewById(R.id.btnGetModel);
        btnGetSN = (Button)findViewById(R.id.btnGetSN);

        tvHAPImsg = (TextView) findViewById(R.id.HAPI_msg);
        txtRegID = (TextView) findViewById(R.id.idText);
        btnDBList = (Button) findViewById(R.id.btnDBRefresh);
        btnDBClear = (Button) findViewById(R.id.btnDBClear);
        btnDBDelete = (Button) findViewById(R.id.btnRCDelete);
        btnEnroll = (Button) findViewById(R.id.btnEnroll);
        btnVerify = (Button) findViewById(R.id.btnVerify);
        btnSearch = (Button) findViewById(R.id.btnSearch);
        viewFinger = (ImageView) findViewById(R.id.imageFinger);
        viewRecord = (ListView)findViewById(R.id.idListView);
        rdoANSI = (RadioButton) findViewById(R.id.rdoANSI); 
        rdoISO = (RadioButton) findViewById(R.id.rdoISO); 

        myThis = this;
		
        m_cLAPI = new LAPI(this);
        m_cHAPI = new HAPI(this,m_fpsdkHandle);

        mHostUSb = new HostUsb(this);

        EnableAllButtons(true,false);
        
        mContext = this;
        mScreenReceiver = new ScreenBroadcastReceiver();
        registerListener();
        //txtRegID.setFocusable(true);
 		
        viewRecord.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> av, View v, final int arg2, long arg3) {
                Runnable r = new Runnable() {
                    int position = arg2;
                    public void run() {
                        SELECT_LIST_ITEM(position);
                    }
                };
                Thread s = new Thread(r);
                s.start();
            }
        });

        btnOpen.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                Runnable r = new Runnable() {
                    public void run() {
                        OPEN_DEVICE();
                    }
                };
                Thread s = new Thread(r);
                s.start();
            }
        });
        
        btnClose.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                Runnable r = new Runnable() {
                    public void run() {
                        CLOSE_DEVICE ();
                    }
                };
                Thread s = new Thread(r);
                s.start();
           }
        });
        
        btnGetImage.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (bContinue) {
                    bContinue = false;
					//btnGetImage.setText("GetImage");
					m_fEvent.sendMessage(m_fEvent.obtainMessage(MESSAGE_SHOW_TEXT, 0, 0,"Canceled"));
                    return;
                }
                btnGetImage.setText("Stop");
                bContinue = true;
                Runnable r = new Runnable() {
                    public void run() {
                        GET_IMAGE ();
                    }
                };
                Thread s = new Thread(r);
				s.start(); 			
            }
        });

        btnOnVideo.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (bContinue) {
                    bContinue = false;
					//btnOnVideo.setText("Video");
					m_fEvent.sendMessage(m_fEvent.obtainMessage(MESSAGE_SHOW_TEXT, 0, 0,"Canceled"));
                    return;
                }
                btnOnVideo.setText("Stop");
                bContinue = true;
                Runnable r = new Runnable() {
                    public void run() {
                        ON_VIDEO ();
                    }
                };
                Thread s = new Thread(r);
				s.start(); 			
            }
        });

        btnCalibration.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
				Runnable r = new Runnable() {
					public void run() {
                        CALIBRATE ();
                    }
				};
				Thread s = new Thread(r);
				s.start();
 			}
        });

        btnGetImageQuality.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                Runnable r = new Runnable() {
                    public void run() {
                        GET_IMAGE_QUALITY ();
                    }
                };
                Thread s = new Thread(r);
                s.start();
            }
        });

        btnGetNFIQuality.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                Runnable r = new Runnable() {
                    public void run() {
                        GET_NFI_QUALITY ();
                    }
                };
                Thread s = new Thread(r);
                s.start();
            }
        });

        btnCreateAnsiTemp.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                Runnable r = new Runnable() {
                    public void run() {
                        CREATE_ANSI_TEMP ();
                    }
                };
                Thread s = new Thread(r);
                s.start();
            }
        });

        btnCreateIsoTemp.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                Runnable r = new Runnable() {
                    public void run() {
                        CREATE_ISO_TEMP ();
                    }
                };
                Thread s = new Thread(r);
                s.start();
            }
        });

        btnCompareAnsiTemp.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                Runnable r = new Runnable() {
                    public void run() {
                        COMPARE_ANSI_TEMP ();
                    }
                };
                Thread s = new Thread(r);
                s.start();
            }
        });
        
        btnCompareIsoTemp.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                Runnable r = new Runnable() {
                    public void run() {
                        COMPARE_ISO_TEMP ();
                    }
                };
                Thread s = new Thread(r);
                s.start();
            }
        });

        btnCompress.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                Runnable r = new Runnable() {
                    public void run() {
                        COMPRESS_TO_WSQ ();
                    }
                };
                Thread s = new Thread(r);
                s.start();
            }
        });

        btnUnCompress.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                Runnable r = new Runnable() {
                    public void run() {
                        UNCOMPRESS_FROM_WSQ ();
                    }
                };
                Thread s = new Thread(r);
                s.start();
            }
        });

        btnGetMake.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                Runnable r = new Runnable() {
                    public void run() {
                        GET_MAKE ();
                    }
                };
                Thread s = new Thread(r);
                s.start();
            }
        });

        btnGetModel.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                Runnable r = new Runnable() {
                    public void run() {
                        GET_MODEL ();
                    }
                };
                Thread s = new Thread(r);
                s.start();
            }
        });

        btnGetSN.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                Runnable r = new Runnable() {
                    public void run() {
                        GET_SN ();
                    }
                };
                Thread s = new Thread(r);
                s.start();
            }
        });

        rdoANSI.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                option = true;
            }
        });
        
        rdoISO.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                option = false;
            }
        });

        btnEnroll.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (bContinue) {
                    m_cHAPI.DoCancel();
					bContinue = false;
					//btnEnroll.setText(String.format("Enroll"));
                    return;
                }
                btnEnroll.setText(String.format("Stop"));
                bContinue = true;
                Runnable r = new Runnable() {
                    public void run() {
                        FINGER_ENROLL ();
                    }
                };
                Thread s = new Thread(r);
                s.start();
            }
        });
		
        btnVerify.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (bContinue) {
                    m_cHAPI.DoCancel();
					bContinue = false;
					//btnVerify.setText(String.format("Verify"));
                    return;
                }
                btnVerify.setText(String.format("Stop"));
                bContinue = true;
                Runnable r = new Runnable() {
                    public void run() {
                        FINGER_VERIFY ();
                    }
                };
                Thread s = new Thread(r);
                s.start();
            }
        });

        btnSearch.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (bContinue) {
                    m_cHAPI.DoCancel();
					bContinue = false;
					//btnSearch.setText(String.format("Search"));
                    return;
                }
                btnSearch.setText(String.format("Stop"));
                bContinue = true;
				m_cHAPI.DBRefresh();
                Runnable r = new Runnable() {
                    public void run() {
                        FINGER_SAERCH ();
                    }
                };
                Thread s = new Thread(r);
                s.start();
            }
        });
		
        btnDBList.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                Runnable r = new Runnable() {
                    public void run() {
                        DB_LIST ();
                    }
                };
                Thread s = new Thread(r);
                s.start();
            }
        });

        btnDBClear.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                new AlertDialog.Builder(myThis)
                    .setIconAttribute(android.R.attr.alertDialogIcon)
                    .setTitle("Clear all records from Database ")
                    .setMessage("Will you clear realy ?")
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {

                        Runnable r = new Runnable() {
                            public void run() {
                                m_cHAPI.ClearALLRecords();
                                m_cHAPI.DBRefresh ();
                                String msg = String.format("Clear DataBase : OK");
                                m_fpsdkHandle.obtainMessage(HAPI.MSG_SHOW_TEXT, 0, 0, msg).sendToTarget();
                            }
                        };
                        Thread s = new Thread(r);
                        s.start();
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                        }
                    })
                    .show();
            }
        });

        btnDBDelete.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                Runnable r = new Runnable() {
                    public void run() {
                        DB_DELETE ();
                    }
                };
                Thread s = new Thread(r);
                s.start();
            }
        });
		
    }
	
    protected void OPEN_DEVICE() {
        String msg;
        m_hDevice = m_cLAPI.OpenDeviceEx();
        if (m_hDevice==0) msg = "Can't open device !";
        else {
            msg = "OpenDevice() = OK";
            EnableAllButtons (false,true);
        }
        m_cHAPI.m_hDev = m_hDevice;
        m_fEvent.sendMessage(m_fEvent.obtainMessage(MESSAGE_SHOW_TEXT, 0, 0,msg));
    }
	
    protected void CLOSE_DEVICE() {
        try {
            String msg;
            m_cHAPI.DoCancel();
            if (m_hDevice != 0) {
                //m_cLAPI.CtrlLed(m_hDevice, 0);	//for Optical
                m_cLAPI.CloseDeviceEx(m_hDevice);
            }
            msg = "CloseDevice() = OK";

            m_hDevice = 0;
            m_cHAPI.m_hDev = 0;
            EnableAllButtons(true, false);
            m_fEvent.sendMessage(m_fEvent.obtainMessage(MESSAGE_SHOW_TEXT, 0, 0, msg));
        } catch (Exception E) {
            E.printStackTrace();
        }
    }

    protected void GET_IMAGE() {
        EnableAllButtons(false,false);
        m_fEvent.sendMessage(m_fEvent.obtainMessage(MESSAGE_ID_ENABLED, R.id.btnGetImage, 1));
		//String msg;
		//m_cLAPI.CtrlLed(m_hDevice, 1);	//for Optical
        m_fEvent.sendMessage(m_fEvent.obtainMessage(MESSAGE_SHOW_TEXT, 0, 0,"Put your finger"));
        m_fEvent.obtainMessage(MESSAGE_SHOW_IMAGE, LAPI.WIDTH, LAPI.HEIGHT, null).sendToTarget();
        while (bContinue) {
            int ret = m_cLAPI.GetImage(m_hDevice, m_image);
			if (ret == LAPI.NOTCALIBRATED) {
				m_fEvent.obtainMessage(MESSAGE_SHOW_TEXT, 0, 0, "not Calibrated !").sendToTarget();
				break;
			}
            if (ret != LAPI.TRUE) {
                m_fEvent.obtainMessage(MESSAGE_SHOW_TEXT, 0, 0, "Can't get image !").sendToTarget();
                break;
            }
            ret = m_cLAPI.IsPressFinger(m_hDevice,m_image);
            if (ret >= LAPI.DEF_FINGER_SCORE) {
				m_fEvent.obtainMessage(MESSAGE_SHOW_TEXT, 0, 0, "GetImage() = OK").sendToTarget();
                m_fEvent.sendMessage(m_fEvent.obtainMessage(MESSAGE_SHOW_IMAGE, LAPI.WIDTH, LAPI.HEIGHT,m_image));
                break;
            }
        }
        bContinue = false;
		//m_cLAPI.CtrlLed(m_hDevice, 0);	//for Optical
		m_appHandle.obtainMessage(MESSAGE_ID_SETTEXT, R.id.btnGetImage, R.string.TEXT_GET_IMAGE).sendToTarget();
        EnableAllButtons(false,true);
    }
	
    protected void ON_VIDEO() {
        EnableAllButtons(false,false);
        m_fEvent.sendMessage(m_fEvent.obtainMessage(MESSAGE_ID_ENABLED, R.id.btnOnVideo, 1));
		//m_cLAPI.CtrlLed(m_hDevice, 1);	//for Optical
        m_fEvent.sendMessage(m_fEvent.obtainMessage(MESSAGE_SHOW_TEXT, 0, 0,"Put your finger"));
        while (bContinue) {
            int startTime = (int)System.currentTimeMillis();
            int ret = m_cLAPI.GetImage(m_hDevice, m_image);
			if (ret == LAPI.NOTCALIBRATED) {
				m_fEvent.obtainMessage(MESSAGE_SHOW_TEXT, 0, 0, "not Calibrated !").sendToTarget();
				break;
			}
            if (ret != LAPI.TRUE) {
                m_fEvent.obtainMessage(MESSAGE_SHOW_TEXT, 0, 0, "Can't get image !").sendToTarget();
                break;
            }
            ret = m_cLAPI.IsPressFinger(m_hDevice,m_image);
			//if (ret >= m_cLAPI.DEF_FINGER_SCORE) {
				//m_cLAPI.CtrlLed(m_hDevice, 0);	//for Optical
            String msg = String.format("GetImage(%d) = OK : %dms", ret, (int)(System.currentTimeMillis() - startTime));
            m_fEvent.sendMessage(m_fEvent.obtainMessage(MESSAGE_SHOW_TEXT, 0, 0,msg));
			//	SLEEP(300);
				//m_cLAPI.CtrlLed(m_hDevice, 1);	//for Optical
			//}
			m_fEvent.obtainMessage(MESSAGE_SHOW_IMAGE, LAPI.WIDTH, LAPI.HEIGHT, m_image).sendToTarget();
        }
		bContinue = false;
		//m_cLAPI.CtrlLed(m_hDevice, 0);	//for Optical
		m_appHandle.obtainMessage(MESSAGE_ID_SETTEXT, R.id.btnOnVideo, R.string.TEXT_VIDEO).sendToTarget();
        EnableAllButtons(false,true);
    }

	protected void CALIBRATE() 
    {
        int ret;
        String msg = "";
		msg = String.format("Calibrating...");
		m_fEvent.sendMessage(m_fEvent.obtainMessage(MESSAGE_SHOW_TEXT, 0, 0,msg));
        EnableAllButtons(false,false);
		sensor_mode++;	    //for Optical
        sensor_mode %= 3;
        //sensor_mode = 0 --> for default,  = 1 --> for wet,  = 2 --> for dry
        ret = m_cLAPI.Calibration(m_hDevice, sensor_mode);
		if (ret == LAPI.TRUE) msg = String.format("Calibration(%d) = OK", sensor_mode);
		else  msg = String.format("Calibration(%d) = Fail", sensor_mode);
        m_fEvent.sendMessage(m_fEvent.obtainMessage(MESSAGE_SHOW_TEXT, 0, 0,msg));
		EnableAllButtons(false,true);
    }

	protected void GET_IMAGE_QUALITY() 
    {
        int qr;
        String msg = "";
        qr = m_cLAPI.GetImageQuality(m_hDevice,m_image);
        msg = String.format("GetImageQuality() = %d", qr);
        m_fEvent.sendMessage(m_fEvent.obtainMessage(MESSAGE_SHOW_TEXT, 0, 0,msg));
    }

	protected void GET_NFI_QUALITY() 
    {
        int qr;
        String msg = "";
        String[] degree = {"excellent","very good","good","poor","fair"};
        qr = m_cLAPI.GetNFIQuality(m_hDevice,m_image);
        msg = String.format("GetNFIQuality() = %d : %s", qr, degree[qr-1]);
        m_fEvent.sendMessage(m_fEvent.obtainMessage(MESSAGE_SHOW_TEXT, 0, 0,msg));
    }

	protected void CREATE_ANSI_TEMP() 
    {
        int i, ret, templateLen = 0;
        String msg, str;
        ret =m_cLAPI.IsPressFinger(m_hDevice,m_image);
        if (ret==0) {
            msg = "IsPressFinger() = 0";
            m_fEvent.sendMessage(m_fEvent.obtainMessage(MESSAGE_SHOW_TEXT, 0, 0,msg));
            return;
        }
        templateLen = m_cLAPI.CreateANSITemplate(m_hDevice,m_image, m_ansi_template);
        if (templateLen == 0) msg = "Can't create ANSI template !";
        else msg = "CreateANSITemplate() = OK";
        m_fEvent.sendMessage(m_fEvent.obtainMessage(MESSAGE_SHOW_TEXT, 0, 0,msg));

        msg = "";
        for (i=0; i < templateLen; i ++) {
            msg += String.format("%02x", m_ansi_template[i]);
        }
        m_fEvent.sendMessage(m_fEvent.obtainMessage(MESSAGE_VIEW_ANSI_TEMPLATE, 0, 0,msg));
 
       if (DEBUG)
        {
            store_cnt++;
            str = String.format("HeroFunFMR_%d(ANSI).bin", store_cnt);
            SaveAsFile (str,  m_ansi_template, templateLen);

            str = String.format("HeroFunFMR_%d(ANSI).txt", store_cnt);
            SaveAsFile (str, msg.getBytes(), templateLen*2);
        }
    }

	protected void CREATE_ISO_TEMP() 
    {
        int i, ret , templateLen = 0;
        String msg, str;
        ret =m_cLAPI.IsPressFinger(m_hDevice,m_image);
        if (ret==0) {
            msg = "IsPressFinger() = 0";
            m_fEvent.sendMessage(m_fEvent.obtainMessage(MESSAGE_SHOW_TEXT, 0, 0,msg));
            return;
        }
        templateLen = m_cLAPI.CreateISOTemplate(m_hDevice,m_image, m_iso_template);
        if (templateLen == 0) msg = "Can't create ISO template !";
        else msg = "CreateISOTemplate() = OK";
        m_fEvent.sendMessage(m_fEvent.obtainMessage(MESSAGE_SHOW_TEXT, 0, 0,msg));

        msg = "";
        for (i=0; i < templateLen; i ++) {
            msg += String.format("%02x", m_iso_template[i]);
        }
        m_fEvent.sendMessage(m_fEvent.obtainMessage(MESSAGE_VIEW_ISO_TEMPLATE, 0, 0,msg));

        if (DEBUG)
        {
            store_cnt++;
            str = String.format("HeroFunFMR_%d(ISO).bin", store_cnt);
            SaveAsFile (str,  m_iso_template, templateLen);

            str = String.format("HeroFunFMR_%d(ISO).txt", store_cnt);
            SaveAsFile (str, msg.getBytes(), templateLen*2);
        }
    }

	protected void COMPARE_ANSI_TEMP() 
    {
        int score;
        String msg;
        score = m_cLAPI.CompareTemplates(m_hDevice,m_ansi_template, m_iso_template);
        msg = String.format("CompareANSITemplates() = %d", score);
        m_fEvent.sendMessage(m_fEvent.obtainMessage(MESSAGE_SHOW_TEXT, 0, 0,msg));
    }

	protected void COMPARE_ISO_TEMP() 
    {
        int score;
        String msg;
        score = m_cLAPI.CompareTemplates(m_hDevice,m_ansi_template, m_iso_template);
        msg = String.format("CompareISOTemplates() = %d", score);
        m_fEvent.sendMessage(m_fEvent.obtainMessage(MESSAGE_SHOW_TEXT, 0, 0,msg));
    }

	protected void COMPRESS_TO_WSQ() 
    {
        long wsqsize = m_cLAPI.CompressToWSQImage (m_hDevice,m_image,bfwsq);
        boolean res = SaveAsFile("image.wsq",bfwsq,(int)wsqsize);
        String msg = "";
		if (res && (wsqsize!=0)) msg = "CompressToWSQImage() = OK.";
        else msg = "CompressToWSQImage() = Fail.";
        m_fEvent.sendMessage(m_fEvent.obtainMessage(MESSAGE_SHOW_TEXT, 0, 0,msg));
    }

	protected void UNCOMPRESS_FROM_WSQ() 
    {
        String msg = "";
        long wsqsize = LoadAsFile("image.wsq",bfwsq);
        if (wsqsize != 0) {
            long imasize = m_cLAPI.UnCompressFromWSQImage (m_hDevice,bfwsq,wsqsize,m_image);
            m_fEvent.sendMessage(m_fEvent.obtainMessage(MESSAGE_SHOW_IMAGE, LAPI.WIDTH, LAPI.HEIGHT,m_image));
			if (imasize != 0) msg = "UnCompressFromWSQImage() = OK."; 
            else msg = "UnCompressFromWSQImage() = Fail.";
        } else msg = "no exist <image.wsq> file.";
        m_fEvent.sendMessage(m_fEvent.obtainMessage(MESSAGE_SHOW_TEXT, 0, 0,msg));
    }

    protected void GET_MAKE ()
    {
        String msg = "";
        String str = m_cLAPI.GetMake(m_hDevice);
        msg = String.format("GetMake() = %s", str);
        m_fEvent.sendMessage(m_fEvent.obtainMessage(MESSAGE_SHOW_TEXT, 0, 0,msg));
    }

    protected void GET_MODEL ()
    {
        String msg = "";
        String str = m_cLAPI.GetModel(m_hDevice);
        msg = String.format("GetModel() = %s", str);
        m_fEvent.sendMessage(m_fEvent.obtainMessage(MESSAGE_SHOW_TEXT, 0, 0,msg));
    }

    protected void GET_SN ()
    {
        String msg = "";
        String str = m_cLAPI.GetSerialNumber(m_hDevice);
        msg = String.format("GetSerialNumber() = %s", str);
        m_fEvent.sendMessage(m_fEvent.obtainMessage(MESSAGE_SHOW_TEXT, 0, 0,msg));
    }

    protected void FINGER_ENROLL() {
        EnableAllButtons(false,false);
        m_appHandle.sendMessage(m_appHandle.obtainMessage(MESSAGE_ID_ENABLED, R.id.btnEnroll, 1));

        String msg = "";
        Resources res = getResources();
        String regid = txtRegID.getText().toString();
        if ((regid==null) || regid.isEmpty()) {
            msg = res.getString(R.string.Insert_ID);
            m_fpsdkHandle.obtainMessage(HAPI.MSG_SHOW_TEXT, 0, 0, msg).sendToTarget();
            EnableAllButtons(false,true);
            m_appHandle.obtainMessage(MESSAGE_ID_SETTEXT, R.id.btnEnroll, R.string.TEXT_ENROLL).sendToTarget();
            bContinue = false;
            return;
        }
    	
        boolean ret = m_cHAPI.Enroll(regid, option);
        if (ret) {
            msg = String.format("Enroll OK (ID=%s)",regid);
            m_cHAPI.DBRefresh ();
        }
        else {
            msg = String.format("Enroll : False : %s",errorMessage(m_cHAPI.GetErrorCode()));
        }
        bContinue = false;
        m_fpsdkHandle.obtainMessage(HAPI.MSG_SHOW_TEXT, 0, 0, msg).sendToTarget();
        m_appHandle.obtainMessage(MESSAGE_ID_SETTEXT, R.id.btnEnroll, R.string.TEXT_ENROLL).sendToTarget();
        EnableAllButtons(false,true);
    }

    protected void FINGER_VERIFY() {
        EnableAllButtons(false,false);
        m_appHandle.sendMessage(m_appHandle.obtainMessage(MESSAGE_ID_ENABLED, R.id.btnVerify, 1));

        int retry;
        String msg = "";
        Resources res = getResources();
        String regid = txtRegID.getText().toString();
    	
        if ((regid==null) || regid.isEmpty()) {
            msg = res.getString(R.string.Insert_ID);
            m_fpsdkHandle.obtainMessage(HAPI.MSG_SHOW_TEXT, 0, 0, msg).sendToTarget();
            m_appHandle.obtainMessage(MESSAGE_ID_SETTEXT, R.id.btnVerify, R.string.TEXT_VERIFY).sendToTarget();
            bContinue = false;
            EnableAllButtons(false,true);
            return;
        }
        for (retry = 0; retry < 10; retry ++  ) {
            boolean ret = m_cHAPI.Verify(regid, option);
            if (ret) {
	           	msg = String.format("Verify OK (ID=%s) : Time(Capture=%dms,Create=%dms,Match=%dms)",
                        regid,m_cHAPI.GetProcessTime(0),m_cHAPI.GetProcessTime(1),m_cHAPI.GetProcessTime(2));
                break;
            }
            else {
                int errCode = m_cHAPI.GetErrorCode();
	    		if (errCode != HAPI.ERROR_NONE && errCode != HAPI.ERROR_LOW_QUALITY) {
	    			msg = String.format("Verify : False : %s",errorMessage(m_cHAPI.GetErrorCode()));
                    break;
                }
            }
    		SLEEP(300);
        }

		if (retry == 10) {
        	msg = String.format("Verify : False : Exceed retry limit");
    	}
        bContinue = false;
        m_fpsdkHandle.obtainMessage(HAPI.MSG_SHOW_TEXT, 0, 0, msg).sendToTarget();
        m_appHandle.obtainMessage(MESSAGE_ID_SETTEXT, R.id.btnVerify, R.string.TEXT_VERIFY).sendToTarget();
        EnableAllButtons(false,true);
    }

    protected void FINGER_SAERCH() {
        EnableAllButtons(false,false);
        m_appHandle.sendMessage(m_appHandle.obtainMessage(MESSAGE_ID_ENABLED, R.id.btnSearch, 1));
        String msg = "";
    	
        while (bContinue) {
            String searched_id = m_cHAPI.Identify(option);
    		int errCode = m_cHAPI.GetErrorCode();
    		if (errCode != HAPI.ERROR_NONE && errCode != HAPI.ERROR_LOW_QUALITY) {
    			msg = String.format("Identify : False : %s",errorMessage(m_cHAPI.GetErrorCode()));
    			m_fpsdkHandle.obtainMessage(HAPI.MSG_SHOW_TEXT, 0, 0, msg).sendToTarget();
    			break;
    		}
			if ( searched_id.equals("") == true ) 
				msg = String.format("Identify False : Time(Capture=%dms,Create=%dms,Match=%dms)",
                        m_cHAPI.GetProcessTime(0),m_cHAPI.GetProcessTime(1),m_cHAPI.GetProcessTime(2));
			else 
				msg = String.format("Identify OK (ID=%s) : Time(Capture=%dms,Create=%dms,Match=%dms)",
                        searched_id,m_cHAPI.GetProcessTime(0),m_cHAPI.GetProcessTime(1),m_cHAPI.GetProcessTime(2));
            m_fpsdkHandle.obtainMessage(HAPI.MSG_SHOW_TEXT, 0, 0, msg).sendToTarget();
    		SLEEP(600);
        }
        bContinue = false;
        m_appHandle.obtainMessage(MESSAGE_ID_SETTEXT, R.id.btnSearch, R.string.TEXT_SEARCH).sendToTarget();
        EnableAllButtons(false,true);
    }

	static class MyFilter implements FilenameFilter{  
		private String type;  
		public MyFilter(String type){  
		this.type = type;  
		}  
		public boolean accept(File dir,String name){  
		return name.endsWith(type);  
		}  
	} 

    protected void DB_LIST() {
        EnableAllButtons(false,false);
        m_cHAPI.DBRefresh();
        EnableAllButtons(false,true);
    }

    void SELECT_LIST_ITEM(int pos)
    {
        m_appHandle.sendMessage(m_appHandle.obtainMessage(MESSAGE_SET_ID, 0, 0,mFiletString[pos]));
    }

    protected void DB_DELETE() {
        String msg;
        String regid = txtRegID.getText().toString();
        boolean ret = m_cHAPI.DeleteRecord(regid);
    	if (ret==true) 
        {
            msg = String.format("DeleteRecord : OK : ID = %s",regid);
            m_cHAPI.DBRefresh ();
        }
        else msg = String.format("DeleteRecord : False : %s",errorMessage(m_cHAPI.GetErrorCode()));
        m_fpsdkHandle.obtainMessage(HAPI.MSG_SHOW_TEXT, 0, 0, msg).sendToTarget();
    }
	
    protected void SLEEP (int waittime)
    {
        int startTime, passTime = 0;
        startTime = (int)System.currentTimeMillis();
        while (passTime < waittime) {
            passTime = (int)System.currentTimeMillis();
            passTime = passTime - startTime;
        }
    }

    public void EnableAllButtons(boolean bOpen, boolean bOther)
    {
        int iOther;
        if (bOpen) m_appHandle.sendMessage(m_appHandle.obtainMessage(MESSAGE_ID_ENABLED, R.id.btnOpenDevice, 1));
        else m_appHandle.sendMessage(m_appHandle.obtainMessage(MESSAGE_ID_ENABLED, R.id.btnOpenDevice, 0));
        if (bOther) iOther = 1; else iOther = 0;
        m_appHandle.sendMessage(m_appHandle.obtainMessage(MESSAGE_ID_ENABLED, R.id.btnCloseDevice, iOther));
        m_appHandle.sendMessage(m_appHandle.obtainMessage(MESSAGE_ID_ENABLED, R.id.btnGetImage, iOther));
        m_appHandle.sendMessage(m_appHandle.obtainMessage(MESSAGE_ID_ENABLED, R.id.btnOnVideo, iOther));
        m_appHandle.sendMessage(m_appHandle.obtainMessage(MESSAGE_ID_ENABLED, R.id.btnCalibration, iOther));
        m_appHandle.sendMessage(m_appHandle.obtainMessage(MESSAGE_ID_ENABLED, R.id.btnGetImageQuality, iOther));
        m_appHandle.sendMessage(m_appHandle.obtainMessage(MESSAGE_ID_ENABLED, R.id.btnGetNFIQuality, iOther));
        m_appHandle.sendMessage(m_appHandle.obtainMessage(MESSAGE_ID_ENABLED, R.id.btnCreateANSITemp, iOther));
        m_appHandle.sendMessage(m_appHandle.obtainMessage(MESSAGE_ID_ENABLED, R.id.btnCreateISOTemp, iOther));
        m_appHandle.sendMessage(m_appHandle.obtainMessage(MESSAGE_ID_ENABLED, R.id.btnCompareANSITemp, iOther));
        m_appHandle.sendMessage(m_appHandle.obtainMessage(MESSAGE_ID_ENABLED, R.id.btnCompareISOTemp, iOther));
        m_appHandle.sendMessage(m_appHandle.obtainMessage(MESSAGE_ID_ENABLED, R.id.btnCompress, iOther));
        m_appHandle.sendMessage(m_appHandle.obtainMessage(MESSAGE_ID_ENABLED, R.id.btnUnCompress, iOther));
        m_appHandle.sendMessage(m_appHandle.obtainMessage(MESSAGE_ID_ENABLED, R.id.btnGetMake, iOther));
        m_appHandle.sendMessage(m_appHandle.obtainMessage(MESSAGE_ID_ENABLED, R.id.btnGetModel, iOther));
        m_appHandle.sendMessage(m_appHandle.obtainMessage(MESSAGE_ID_ENABLED, R.id.btnGetSN, iOther));
        m_appHandle.sendMessage(m_appHandle.obtainMessage(MESSAGE_ID_ENABLED, R.id.btnEnroll, iOther));
        m_appHandle.sendMessage(m_appHandle.obtainMessage(MESSAGE_ID_ENABLED, R.id.btnVerify, iOther));
        m_appHandle.sendMessage(m_appHandle.obtainMessage(MESSAGE_ID_ENABLED, R.id.btnSearch, iOther));
        m_appHandle.sendMessage(m_appHandle.obtainMessage(MESSAGE_ID_ENABLED, R.id.btnDBRefresh, iOther));
        m_appHandle.sendMessage(m_appHandle.obtainMessage(MESSAGE_ID_ENABLED, R.id.btnDBClear, iOther));
        m_appHandle.sendMessage(m_appHandle.obtainMessage(MESSAGE_ID_ENABLED, R.id.btnRCDelete, iOther));
    }

    public void UpdateListView() {
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, mListString);
        viewRecord.setAdapter(adapter);
    }

    private final Handler m_fEvent = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_SHOW_TEXT:
                    tvLAPImsg.setText((String)msg.obj);
                    break;
                case MESSAGE_VIEW_ANSI_TEMPLATE:
                    tvANSITemp.setText((String)msg.obj);
                    break;
                case MESSAGE_VIEW_ISO_TEMPLATE:
                    tvISOTemp.setText((String)msg.obj);
                    break;
                case MESSAGE_ID_ENABLED:
                    Button btn = (Button) findViewById(msg.arg1);
                    if (msg.arg2 != 0) btn.setEnabled(true);
                    else btn.setEnabled(false);
                    break;
                case MESSAGE_SHOW_IMAGE:
                    ShowFingerBitmap ((byte[])msg.obj,msg.arg1,msg.arg2);
                    break;
            }
        }
    };

    private final Handler m_appHandle = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_SET_ID:
                    txtRegID.setText((String)msg.obj);
                    break;
                case MESSAGE_SHOW_TEXT:
                    tvHAPImsg.setText((String)msg.obj);
                    break;
                case MESSAGE_SHOW_IMAGE:
                    ShowFingerBitmap ((byte[])msg.obj,msg.arg1,msg.arg2);
                    break;
                case MESSAGE_ENABLE_BTN:
                    boolean bEnable = msg.arg1 == 1 ? true : false;
                    boolean bOpen = msg.arg2 == 1 ? true : false;
                    TextView id = (TextView) findViewById(R.id.idText);
                    id.setEnabled(bEnable);
                    Button btn = (Button) findViewById(R.id.btnVerify);
                    btn.setEnabled(bEnable);
                    btn = (Button) findViewById(R.id.btnSearch);
                    btn.setEnabled(bEnable);
                    btn = (Button) findViewById(R.id.btnDBRefresh);
                    btn.setEnabled(bEnable);
                    btn = (Button) findViewById(R.id.btnRCDelete);
                    btn.setEnabled(bEnable);
                    btn = (Button) findViewById(R.id.btnDBClear);
                    btn.setEnabled(bEnable);
                    btn = (Button) findViewById(R.id.btnEnroll);
                    btn.setEnabled(bEnable);
                    btn = (Button) findViewById(R.id.btnOpenDevice);
                    btn.setEnabled(bOpen);
                    break;
                case MESSAGE_SHOW_BITMAP:
                    viewFinger.setImageBitmap((Bitmap)msg.obj);
                    break;
                case MESSAGE_LIST_START:
                    mListString = new String[msg.arg1];
                    mFiletString = new String[msg.arg1];
                    break;
                case MESSAGE_LIST_NEXT:
                    mListString[msg.arg2] = String.format("No = %d : ID = %s",msg.arg2,(String)msg.obj);
                    mFiletString[msg.arg2] = (String)msg.obj;
                    break;
                case MESSAGE_LIST_END:
                    UpdateListView();
                    String txt = String.format("Record Count = %d", msg.arg1);
                    tvHAPImsg.setText(txt);
                    break;
                case MESSAGE_ID_ENABLED:
                    btn = (Button) findViewById(msg.arg1);
                    if (msg.arg2 != 0) btn.setEnabled(true);
                    else btn.setEnabled(false);
                    break;
                case MESSAGE_ID_SETTEXT:
                    btn = (Button) findViewById(msg.arg1);
                    btn.setText(msg.arg2);
                    break;
            }
        }
    };

    private final Handler m_fpsdkHandle = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            String str = "";
            Resources res;
            switch (msg.what) {
                case 0xff:
                    break;
                case HAPI.MSG_SHOW_TEXT:
                    tvHAPImsg.setText((String)msg.obj);
                    break;
                case HAPI.MSG_PUT_FINGER:
                    res = getResources();
                    str = res.getString(R.string.Put_your_finger);
                    if (msg.arg1>0) {
                        str += (" ("+String.valueOf(msg.arg1)+"/"+String.valueOf(msg.arg2)+")");
                    }
                    str += " ! ";
                    str += (String)msg.obj;
                    tvHAPImsg.setText(str);
                    break;
                case HAPI.MSG_RETRY_FINGER:
                    res = getResources();
                    str = res.getString(R.string.Retry_your_finger);
                    str += " !";
                    tvHAPImsg.setText(str);
                    break;
                case HAPI.MSG_TAKEOFF_FINGER:
                    res = getResources();
                    str = res.getString(R.string.Takeoff_your_finger);
                    str += " !";
                    tvHAPImsg.setText(str);
                    break;
                case HAPI.MSG_ON_SEARCHING:
                    res = getResources();
                    str = res.getString(R.string.TEXT_ON_SEARCHING);
                    if (msg.arg1>0) {
                        str += (" (quality="+String.valueOf(msg.arg1)+")");
                    }
                    str += "  ...  ";
                    tvHAPImsg.setText(str);
                    break;
                case HAPI.MSG_FINGER_CAPTURED:
                    ShowFingerBitmap ((byte[])msg.obj,msg.arg1,msg.arg2);
                    break;
                case HAPI.MSG_DBRECORD_START:
                    mListString = new String[msg.arg1];
                    mFiletString = new String[msg.arg1];
                    break;
                case HAPI.MSG_DBRECORD_NEXT:
                    mListString[msg.arg2] = String.format("No = %d : ID = %s",msg.arg2,(String)msg.obj);
                    mFiletString[msg.arg2] = (String)msg.obj;
                    break;
                case HAPI.MSG_DBRECORD_END:
                    UpdateListView();
                    String txt = String.format("Record Count = %d", msg.arg1);
                    tvHAPImsg.setText(txt);
                    break;
            }
        }
    };
    public String errorMessage (int errCode) {
        Resources res;
        res = getResources();
        switch (errCode) {
            case HAPI.ERROR_NONE:
                return res.getString(R.string.ERROR_NONE);
            case HAPI.ERROR_ARGUMENTS:
                return res.getString(R.string.ERROR_ARGUMENTS);
            case HAPI.ERROR_LOW_QUALITY:
                return res.getString(R.string.ERROR_LOW_QUALITY);
            case HAPI.ERROR_NEG_ACCESS:
                return res.getString(R.string.ERROR_NEG_ACCESS);
            case HAPI.ERROR_NEG_FIND:
                return res.getString(R.string.ERROR_NEG_FIND);
            case HAPI.ERROR_NEG_DELETE:
                return res.getString(R.string.ERROR_NEG_DELETE);
            case HAPI.ERROR_INITIALIZE:
                return res.getString(R.string.ERROR_INITIALIZE);
            case HAPI.ERROR_CANT_GENERATE:
                return res.getString(R.string.ERROR_CANT_GENERATE);
            case HAPI.ERROR_OVERFLOW_RECORD:
                return res.getString(R.string.ERROR_OVERFLOW_RECORD);
            case HAPI.ERROR_NEG_ADDNEW:
                return res.getString(R.string.ERROR_NEG_ADDNEW);
            case HAPI.ERROR_NEG_CLEAR:
                return res.getString(R.string.ERROR_NEG_CLEAR);
            case HAPI.ERROR_NONE_ACTIVITY:
                return res.getString(R.string.ERROR_NONE_ACTIVITY);
            case HAPI.ERROR_NONE_CAPIMAGE:
                return res.getString(R.string.ERROR_NONE_CAPIMAGE);
		case HAPI.ERROR_NOT_CALIBRATED:
			return res.getString(R.string.ERROR_NOT_CALIBRATED);
            case HAPI.ERROR_NONE_DEVICE:
                return res.getString(R.string.ERROR_NONE_DEVICE);
            case HAPI.ERROR_TIMEOUT_OVER:
                return res.getString(R.string.ERROR_TIMEOUT_OVER);
            case HAPI.ERROR_DO_CANCELED:
                return res.getString(R.string.ERROR_DOCANCELED);
            case HAPI.ERROR_EMPTY_DADABASE:
                return res.getString(R.string.ERROR_EMPTY_DADABASE);
            default:
                return String.format("errCode=%d", errCode);
        }
    }

    public boolean SaveAsFile (String filename, byte[] buffer, int len) {
        boolean ret = true;
        //File extStorageDirectory = Environment.getExternalStorageDirectory();
        //File Dir = new File(extStorageDirectory, "Android");
        File Dir = mContext.getExternalFilesDir(null);
        File file = new File(Dir, filename);
        try { 
            FileOutputStream out = new FileOutputStream(file);                    
            out.write(buffer,0,len);
            out.close();
         } catch (Exception e) { 
            ret = false;
        }
        return ret;
    }

    public long LoadAsFile (String filename, byte[] buffer) {
        long ret = 0;
        //File extStorageDirectory = Environment.getExternalStorageDirectory();
        //File Dir = new File(extStorageDirectory, "Android");
        File Dir = mContext.getExternalFilesDir(null);
        File file = new File(Dir, filename);
        ret = file.length();
        try { 
            FileInputStream out = new FileInputStream(file);                    
            out.read(buffer);
            out.close();
         } catch (Exception e) { 
        }
        return ret;
    }

    private void ShowFingerBitmap(byte[] image, int width, int height) {
        if (width==0) return;
        if (height==0) return;
        for (int i = 0; i < width * height; i++ ) {
            int v;
            if (image != null) v = image[i] & 0xff;
            else v= 0;
            RGBbits[i] = Color.rgb(v, v, v);
        }
        Bitmap bmp = Bitmap.createBitmap(RGBbits, width, height,Config.RGB_565);
        viewFinger.setImageBitmap(bmp);
    }

    public int RemoveExData(byte[] data, int len){
        if (len == 0) return 0;
        int i0, j0, i;
        j0 = (data[27] & 0xFF);									//number of minutia
        i0 = j0*6 + 28;											//start address of extended data;
        j0 = ((data[i0] & 0xFF) <<8) | (data[i0+1] & 0xFF);		//length of extended data
        data[i0] = (byte)0x00;
        data[i0+1] = (byte)0x00;
        i0 = ((data[8] & 0xFF) <<24) | ((data[9] & 0xFF) <<16)	| ((data[10] & 0xFF) <<8) | (data[11] & 0xFF);
        //length of record
        i0 -= j0;
        data[11] = (byte)(i0 & 0xFF);
        data[10] = (byte)((i0 & 0xFF00)>>8);
        data[9] = (byte)((i0 & 0xFF0000)>>16);
        data[8] = (byte)((i0 & 0xFF000000)>>24);
        for (i = 0; i < j0; i++) data[i0+i] = (byte)0x00;
        return i0;
    }
}