/**
 * 
 */
package de.paraair.ardmix;

import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import de.sciss.net.OSCClient;
import de.sciss.net.OSCListener;
import de.sciss.net.OSCMessage;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.text.Format;
import java.util.HashMap;

/**
 * @author lincoln
 *
 */
public class OscService {

	private static String TAG = "OscService";

    private Integer masterId = 513;

	// States
	private static final int READY = 0;

	// Transport actions
	public static final int TRANSPORT_PLAY = 0;
	public static final int TRANSPORT_STOP = 1;
	public static final int GOTO_START = 2;
	public static final int GOTO_END = 3;
	public static final int REC_ENABLE_TOGGLE = 4;
	public static final int LOOP_ENABLE_TOGGLE = 5;
	public static final int FFWD = 6;
	public static final int REWIND = 7;
	public static final int LOCATE = 8;

	public static final int GOTO_PREV_MARKER = 9;
	public static final int ADD_MARKER = 10;
	public static final int GOTO_NEXT_MARKER = 11;

	public static final int ALL_REC_ENABLE = 20;
	public static final int ALL_REC_DISABLE = 21;
	public static final int ALL_REC_TOGGLE = 22;
	private static final int ALL_MUTE_ENABLE_TOGGLE = 13;

    public static final int ALL_STRIPIN_ENABLE = 30;
    public static final int ALL_STRIPIN_DISABLE = 31;
    public static final int ALL_STRIPIN_TOGGLE = 32;

	// Change Ids
	public static final int REC_CHANGED = 0;
	public static final int MUTE_CHANGED = 1;
	public static final int SOLO_CHANGED = 2;
    public static final int SOLO_ISOLATE_CHANGED = 51;
	public static final int SOLO_SAFE_CHANGED = 52;
	public static final int ROUTES_REQUESTED = 4;
	public static final int NAME_CHANGED = 3;
    public static final int GAIN_CHANGED = 4;
	public static final int FADER_CHANGED = 6;
    public static final int AUX_CHANGED = 7;
	public static final int RECEIVE_CHANGED = 8;
	public static final int SEND_CHANGED = 9;
	public static final int SEND_ENABLED = 10;


	public static final int STRIPIN_CHANGED = 5;

	public static final int PLUGIN_RESET = 20;

	private OSCClient oscClient;


	private HashMap<Integer, Track> routes= new HashMap<>();
//	private ArrayList<Track> routes= new ArrayList<>();

	private int state = 0;
	
	private String host = "127.0.0.1";
	private int port = 3819;
	private String protocol = "udp";


	static final int MSG_SAY_HELLO = 1;


	/** The handler where we shall post transport state updates on the UI thread. */
	private Handler transportHandler = null;


	//private ArdroidMain ardroidMainActivity = null;

	public OscService() {

	}

	/**
	 * 
	 * @param host The host name or IP address to be connected
	 * @param port The port to connect to
	 */
	public OscService(String host, int port){
		this.host = host;
		this.port = port;
	}




	/**
	 * @return the transportHandler
	 */
	public Handler getTransportHandler() {
		return transportHandler;
	}

	/**
	 * @param transportHandler the transportHandler to set
	 */
	public void setTransportHandler(Handler transportHandler) {
		this.transportHandler = transportHandler;
	}
	
	//public void setListener(ArdroidMain at){
	//	this.ardroidMainActivity = at;
	//}
	
	/**
	 * Connect to the Ardour OSC server
	 */
	public boolean connect(){
		
		try {

			Log.d(TAG, "Connetecting to Ardour");
			
			if(oscClient != null && oscClient.isConnected()){
				disconnect();
			}

	        oscClient = OSCClient.newUsing (protocol);    // create UDP client with any free port number
	        oscClient.setTarget (new InetSocketAddress(InetAddress.getByName(host), port));
	        	        
	        Log.d(TAG, "Starting connection...");

			oscClient.start();  // open channel and (in the case of TCP) connect, then start listening for replies
			
	        Log.d(TAG, "Started. Starting listener");



	        oscClient.addOSCListener(replyListener);
	        
	        Log.d(TAG, "Listening.");
			

		} catch (UnknownHostException e) {
			Log.d(TAG, "Unknown host");
			e.printStackTrace();
            return false;
		} catch (IOException e) {
			Log.d(TAG, "IO Exception");
			e.printStackTrace();
            return false;
		}
        return true;
	}

    public void initSurfaceFeedback1() {
//        if (oscClient.isConnected()){
//
//            int feedback = 0; // feedback
//
//            System.out.println("OSC State: " + state);
//            Object[] args = {
//					0 // bank size
//					, ArdourConstants.STRIP_TRACK_AUDIO // strip types
//					+ ArdourConstants.STRIP_HIDDEN
//					+ ArdourConstants.STRIP_BUS_AUDIO
//					+ ArdourConstants.STRIP_AUX
////                    + ArdourConstants.STRIP_MASTER
//					, feedback
//					, 1 // fader mode (float from 0 to 1)
//			};
//
//			sendOSCMessage("/set_surface", args);
//
//        }
    }

	public void initSurfaceFeedback2() {
		if (oscClient.isConnected()){

			int feedback = ArdourConstants.FEEDBACK_STRIP_BUTTONS // feedback
                    + ArdourConstants.FEEDBACK_STRIP_VALUES
					+ ArdourConstants.FEEDBACK_MASTER
					+ ArdourConstants.FEEDBACK_STRIP_METER_16BIT
                    + ArdourConstants.FEEDBACK_TIMECODE
                    + ArdourConstants.FEEDBACK_TRANSPORT_POSITION_SAMPLES
					+ ArdourConstants.FEEDBACK_EXTRA_SELECT
					;

			System.out.println("OSC State: " + state);
			Object[] args = {
					0 // bank size
					, ArdourConstants.STRIP_TRACK_AUDIO // strip types
					+ ArdourConstants.STRIP_HIDDEN
					+ ArdourConstants.STRIP_BUS_AUDIO
					+ ArdourConstants.STRIP_MASTER
                    + ArdourConstants.STRIP_VCA
					+ ArdourConstants.STRIP_AUX
					, feedback
					, 1 // fader mode (loat from 0 to 1)
			};

			sendOSCMessage("/set_surface", args);

		}
	}


	public void requestStripList() {
		if (oscClient.isConnected()){

			routes.clear();
			state = OscService.ROUTES_REQUESTED;
//			try {
//				Thread.sleep(500);
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
			sendOSCMessage("/strip/list");

			Object[] args = {1};
			sendOSCMessage("/select/expand", args);
		}
	}

	/**
	 * Method to disconnect from the present connected session.
	 * Ask Ardour to stop listening to our observed tracks.
	 */
	public void disconnect(){

        Log.d(TAG, "Disconnetecting from Ardour");

//        Object[] sargs = {
//                0 // bank size
//                , ArdourConstants.STRIP_TRACK_AUDIO // strip types
//                + ArdourConstants.STRIP_BUS_AUDIO
//                , 0
//                , 1 // fader mode (loat from 0 to 1)
//        };
//        sendOSCMessage("/set_surface", sargs);
//
//        Integer[] args = new Integer[routes.size()];
//		int j = 0;
//		for( int index : routes.keySet()) {
//			args[j++] = index;
//        }
//        sendOSCMessage("/strip/ignore", args);

        oscClient.dispose();
	}
	
	/**
	 * Check if the OSC client is connected
	 * @return Flag indicates OSC connction state
	 */
	public boolean isConnected(){
		
		if (oscClient != null){
			return oscClient.isConnected();
		}
		
		return false;
	}
	
	/**
	 * Map an OSC URI path with the Transport Action in the UI
	 * @param cmd The URI of the action to be performed
	 */
	public void transportAction(int cmd){
		
		String uri = "";
		
		switch(cmd){
		case TRANSPORT_PLAY:
			uri = "/transport_play";
			break;
			
		case TRANSPORT_STOP:
			uri = "/transport_stop";
			break;
			
		case GOTO_START:
			uri = "/goto_start";
			break;
			
		case GOTO_END:
			uri = "/goto_end";
			break;
			
		case REC_ENABLE_TOGGLE:
			uri = "/rec_enable_toggle";
			break;

		case ALL_REC_ENABLE:
			for( int index : routes.keySet()) {
				Track t = routes.get(index);
				if( t.type == Track.TrackType.AUDIO && !t.recEnabled) {
					trackListAction(REC_CHANGED, t);
				}
			}
			uri = "";
			break;

		case ALL_REC_DISABLE:
			for( int index : routes.keySet()) {
				Track t = routes.get(index);
				if( t.type == Track.TrackType.AUDIO && t.recEnabled) {
					trackListAction(REC_CHANGED, t);
				}
			}
			uri = "";
			break;

		case ALL_REC_TOGGLE:
			for( int index : routes.keySet()) {
				Track t = routes.get(index);
				if( t.type == Track.TrackType.AUDIO ) {
					trackListAction(REC_CHANGED, t);
				}
			}
			uri = "";
			break;

        case ALL_STRIPIN_ENABLE:
			for( int index : routes.keySet()) {
				Track t = routes.get(index);
                if( t.type == Track.TrackType.AUDIO && !t.stripIn) {
                    trackListAction(STRIPIN_CHANGED, t);
                }
            }
            uri = "";
            break;

        case ALL_STRIPIN_DISABLE:
			for( int index : routes.keySet()) {
				Track t = routes.get(index);
                if( t.type == Track.TrackType.AUDIO && t.stripIn ) {
                    trackListAction(STRIPIN_CHANGED, t);
                }
            }
            uri = "";
            break;

        case ALL_STRIPIN_TOGGLE:
			for( int index : routes.keySet()) {
				Track t = routes.get(index);
                if( t.type == Track.TrackType.AUDIO ) {
                    trackListAction(STRIPIN_CHANGED, t);
                }
            }
            uri = "";
            break;

            case ALL_MUTE_ENABLE_TOGGLE:
            uri = "/toggle_all_rec_enables";
			break;

		case LOOP_ENABLE_TOGGLE:
			uri = "/ardour/loop_toggle";
			break;
			
		case FFWD:
			uri = "/ardour/ffwd";
			break;
			
		case REWIND:
			uri = "/ardour/rewind";
			break;

		case GOTO_PREV_MARKER:
			uri = "/ardour/prev_marker";
			break;

		case ADD_MARKER:
			uri = "/ardour/add_marker";
			break;

		case GOTO_NEXT_MARKER:
			uri = "/ardour/next_marker";
			break;
		}

        if( !uri.equals(""))
		    sendOSCMessage(uri);
	}

	/**
	 * Send a track list event to Ardour
	 * @param cmd the command to be performed
	 * @param i the index of the track receivs the action
	 * @param keep Flag to control running state on Ardour side
	 */
	public void transportAction(int cmd, int i, boolean keep){
		
		String uri = "";
		
		Integer[] args = new Integer[2];
		args[0] = i;
		
		switch(cmd){
		case LOCATE:
			uri = "/locate";
			args[1] = keep ? 1 : 0;

			break;
		}
		
		sendOSCMessage(uri, args);
	}

//	/**
//	 * Send a track list event to Ardour
//	 * @param track
//	 * @param position
//	 */
//    public void trackListVolumeAction( Track track, int position){
//
////		String uri = "/ardour/routes/gainabs";
//		String uri = "/strip/fader";
//
//		Object[] args = new Object[2];
//            args[0] = Integer.valueOf(track.remoteId);
//
//            track.trackVolume = position;
//			//args[1] = Double.valueOf(track.sliderToValue(position) / 1000.0);
//			args[1] = (float)(position / 1000.0);
//			//System.out.printf("pos: %d, args[1]: %f\n", position, (float)args[1]);
//
//            sendOSCMessage(uri, args);
//    }


	public void recvListVolumeAction( Track track, int sendId, int newVol){

		String uri = "/strip/send/fader";

		Object[] args = new Object[3];
		args[0] = track.remoteId;

		args[1] = sendId;

//		track.trackVolume = position;
		args[2] = newVol / 1000.0;

		sendOSCMessage(uri, args);
	}

	public void panAction( Track track ){

		String uri = "/strip/pan_stereo_position";

		Object[] args = new Object[2];
		args[0] = track.remoteId;

		args[1] = track.panPosition;

		sendOSCMessage(uri, args);
	}

	/**
	 * Send a track list event to Ardour
	 * @param cmd The command to be performed on the track
	 * @param track the index of the track to take the action
	 */
	public void trackListAction(int cmd, Track track){
		
		String uri = "";

		Object[] args = new Object[2];
		args[0] = track.remoteId;
		
        String stripURI = "/strip";
        int argValueIndex = 1;

        if( track.remoteId == masterId ) {
            args = new Object[1];
            stripURI = "/master";
            argValueIndex = 0;
        }
		
		switch(cmd){
            case REC_CHANGED:
                uri = "/recenable";
                args[argValueIndex] = track.recEnabled ? 0 : 1;

                break;

            case MUTE_CHANGED:
                uri = "/mute";
                args[argValueIndex] = track.muteEnabled ? 0 : 1;

                break;

			case SOLO_CHANGED:
				uri = "/solo";
				args[argValueIndex] = track.soloEnabled ? 0 : 1;

				break;

			case SOLO_ISOLATE_CHANGED:
				uri = "/solo_iso";
				args[argValueIndex] = track.soloIsolateEnabled ? 0 : 1;

				break;

			case SOLO_SAFE_CHANGED:
				uri = "/solo_safe";
				args[argValueIndex] = track.soloSafeEnabled ? 0 : 1;

				break;

			case STRIPIN_CHANGED:
                uri = "/monitor_input";
                args[argValueIndex] = track.stripIn ? 0 : 1;

                break;

			case FADER_CHANGED:
				uri = "/fader";
				args[argValueIndex] = (float) track.trackVolume / 1000;
				break;

            case AUX_CHANGED:
                args = new Object[3];
                stripURI = "/strip/send";
                uri = "/fader";
                args[0] = track.remoteId;
                args[1] = track.source_id;
                args[2] = (float) track.currentSendVolume / 1000;
                break;

            case NAME_CHANGED:
				uri = "/name";
				args[argValueIndex] = track.name;
				break;
		}

		sendOSCMessage(stripURI + uri, args);
	}


	/**
	 * Send a message with no arguments.
	 * @param messageUri The URI of the message to be send
	 */
	private void sendOSCMessage(String messageUri){
		this.sendOSCMessage(messageUri, null);
	}
	

	/**
	 * Send an OSC message over the OSC socket.
	 * @param messageUri The URI of the message to be send
     * @param args the arguments passed with the message
	 */
	private void sendOSCMessage(final String messageUri, final Object[] args){

        OSCMessage message;

        if (args == null || args.length == 0){
            message = new OSCMessage(messageUri);
        }
        else {
            message = new OSCMessage(messageUri, args);
        }

        try {
            oscClient.send (message);
        }
        catch (IOException e){
            Log.d(TAG, "Could not send OSC message: " + messageUri);
            e.printStackTrace();
        }

	}


    private Handler queueHandler = new Handler() {

        @Override
        public void handleMessage(Message queueMessage) {
            OSCMessage message = (OSCMessage)queueMessage.obj;
            msg_count++;
            if( msg_count > msg_max)
                msg_max = msg_count;

            if ( message.getName().equals("/rec_enable_toggle")) {
                System.out.printf("path: %s\n", message.getName());
            }

            if( message.getName().equals("#reply") ) {
                String arg0 = (String)message.getArg(0);
                if(arg0.equals("end_route_list")) {
                    System.out.printf("#end-route-list\n");

                    Long frameRate = (Long) message.getArg(1);

                    Message msg = transportHandler.obtainMessage(ArdourConstants.OSC_FRAMERATE, frameRate);
                    transportHandler.sendMessage(msg);

                    Long maxFrame = (Long) message.getArg(2);

                    Message msg1 = transportHandler.obtainMessage(ArdourConstants.OSC_MAXFRAMES, maxFrame);
                    transportHandler.sendMessage(msg1);

                    Message msg2 = transportHandler.obtainMessage(ArdourConstants.OSC_STRIPLIST);
                    transportHandler.sendMessage(msg2);
                    state = OscService.READY;

                    return;
                }

                Track t = new Track();

                switch (arg0) {
                    case "AT":
                        t.type = Track.TrackType.AUDIO;
                        break;
                    case "MT":
                        t.type = Track.TrackType.MIDI;
                        break;
                    case "B":
                        t.type = Track.TrackType.BUS;
                        break;
					case "M":
					case "MA":
						t.type = Track.TrackType.MASTER;
						masterId = (Integer) message.getArg(6);
						break;
                    case "V":;
                        t.type = Track.TrackType.VCA;
                        break;
                }


                //Set the name of the track
                t.name = (String) message.getArg(1);

                //Set mute state
                Integer i = (Integer) message.getArg(4);
                t.muteEnabled = (i > 0);

                //Set solo state
                i = (Integer) message.getArg(5);
                t.soloEnabled = (i > 0);

                //Set remote id
                i = (Integer) message.getArg(6);
                t.remoteId = i;


                //Set Volume
                if( message.getArgCount() > 8 ) {
                    Float rawVol = (Float)message.getArg(8);
                    t.trackVolume = (int)(rawVol*1000);
                }

                if( message.getArgCount() > 9 ) {
                    t.sendCount = (Integer) message.getArg(9);
                }

                //Set record state
                if(t.type == Track.TrackType.AUDIO || t.type == Track.TrackType.MIDI){
                    i = (Integer) message.getArg(7);
                    t.recEnabled = (i > 0);
                }

				if( !routes.containsKey(t.remoteId) ) {
					routes.put(t.remoteId, t);
					Message msg3 = transportHandler.obtainMessage(ArdourConstants.OSC_NEWSTRIP, t);
					transportHandler.sendMessage(msg3);
				}
            }
            else {

                // get a list of the URI elements
                String[] pathes = message.getName().split("/");
                Integer stripIndex = 0;
                Track t;

                switch (pathes[1]) {
					case "expand":
						if(pathes.length < 3) {
							System.out.printf("pathes.length < 3: %s, ", message.getName());
							for( int a = 0; a < message.getArgCount(); a++) {
								System.out.printf("%d-%s,  ", a, String.valueOf(message.getArg(a)));
							}
							System.out.printf("\n");
							return;
						}
						break;
                    case "transport_play":
                        transportHandler.sendMessage(transportHandler.obtainMessage(ArdourConstants.OSC_PLAY, (int)message.getArg(0), 0));
                        break;

                    case "transport_stop":
                        transportHandler.sendMessage(transportHandler.obtainMessage(ArdourConstants.OSC_STOP, (int)message.getArg(0), 0));
                        break;

                    case "rec_enable_toggle":
                        transportHandler.sendMessage(transportHandler.obtainMessage(ArdourConstants.OSC_RECORD, (int)message.getArg(0), 0));
                        break;

                    case "select":
                        switch (pathes[2]) {
                            case "send_fader":
                                Object margs[] = new Object[message.getArgCount()];
                                for(int i = 0; i < message.getArgCount(); i++ ) {
                                    margs[i] = message.getArg(i);
                                }
                                transportHandler.sendMessage(transportHandler.obtainMessage(ArdourConstants.OSC_SELECT_SEND_FADER, margs));
                                break;

                            case "send_enable":
                                Object seargs[] = new Object[message.getArgCount()];
                                for(int i = 0; i < message.getArgCount(); i++ ) {
                                    seargs[i] = message.getArg(i);
                                }
                                transportHandler.sendMessage(transportHandler.obtainMessage(ArdourConstants.OSC_SELECT_SEND_ENABLE, seargs));
                                break;

                            case "send_name":
                                transportHandler.sendMessage(transportHandler.obtainMessage(
                                        ArdourConstants.OSC_SELECT_SEND_NAME, (int)message.getArg(0), 0, message.getArg(1)));
                                break;
                            default:
                                break;
                        }
                        break;
                    case "strip":
                        // argOffset represents if the strip index is part of the URI (argOffset=1) or not (argOffset=0)
                        int argOffset = 0;
                        if (pathes.length > 3 && TextUtils.isDigitsOnly(pathes[3]) )
                            stripIndex = Integer.parseInt(pathes[3]);
                        else {
                            if( message.getArgCount() > 0) {
                                stripIndex = (int) message.getArg(0);
                                argOffset = 1;
                            }
                        }
                        switch (pathes[2]) {
							case "list":
                                transportHandler.sendMessage(transportHandler.obtainMessage(ArdourConstants.OSC_RECONNECT));
								Log.d(TAG, "list");
								break;
                            case "processors":
                                Log.d(TAG, "processors");
                                break;
                            case "plugin":
                                switch(pathes[3]) {
                                    case "list":

                                        Object plargs[] = new Object[message.getArgCount()];
                                        for(int i = 0; i < message.getArgCount(); i++ ) {
                                            plargs[i] = message.getArg(i);
                                        }
                                        transportHandler.sendMessage(transportHandler.obtainMessage(ArdourConstants.OSC_PLUGIN_LIST, plargs));
                                        break;
                                    case "descriptor":
                                        Object pdargs[] = new Object[message.getArgCount()];
                                        for(int i = 0; i < message.getArgCount(); i++ ) {
                                            pdargs[i] = message.getArg(i);
                                        }
                                        transportHandler.sendMessage(transportHandler.obtainMessage(ArdourConstants.OSC_PLUGIN_DESCRIPTOR, pdargs));
                                        break;
									case "descriptor_end":
										Object pdeargs[] = new Object[message.getArgCount()];
										for(int i = 0; i < message.getArgCount(); i++ ) {
											pdeargs[i] = message.getArg(i);
										}
										transportHandler.sendMessage(transportHandler.obtainMessage(ArdourConstants.OSC_PLUGIN_DESCRIPTOR_END, pdeargs));
										break;
                                }
                                break;
                            case "meter":
                                t = getTrack(stripIndex);

                                if (message.getArg(1) instanceof Integer) {
                                    int newMeter = ((int)message.getArg(1));

                                    newMeter = (newMeter != 0xffff) ? newMeter & 0x1FFF : 0;
                                    if( t != null && t.meter != newMeter ) {
                                        t.meter = newMeter;
                                        transportHandler.sendMessage(transportHandler.obtainMessage(ArdourConstants.OSC_STRIP_METER, stripIndex, 0));
                                    }
                                }
                                break;
                            case "receives":

                                Object margs[] = new Object[message.getArgCount()];
                                for(int i = 0; i < message.getArgCount(); i++ ) {
                                    margs[i] = message.getArg(i);
                                }
                                transportHandler.sendMessage(transportHandler.obtainMessage(ArdourConstants.OSC_STRIP_RECEIVES, margs));
                                break;

                            case "sends":

                                Object rargs[] = new Object[message.getArgCount()-1];

                                for(int i = 1; i < message.getArgCount(); i++ ) {
                                    rargs[i-1] = message.getArg(i);
                                }
                                transportHandler.sendMessage(transportHandler.obtainMessage(ArdourConstants.OSC_STRIP_SENDS, (int)message.getArg(0), 0, rargs));
                                break;
                            case "name":
                                t = getTrack(stripIndex);
                                if (t!=null) {
                                    t.name = (String) message.getArg(argOffset);
                                    transportHandler.sendMessage(transportHandler.obtainMessage(ArdourConstants.OSC_STRIP_NAME, stripIndex, 0));
                                }
                                break;
                            case "fader":
                            	if (pathes.length > 3) {
									Log.d(TAG, "too long path");
									break;
								}
                                t = getTrack(stripIndex);
                                if ( t!=null && !t.getTrackVolumeOnSeekBar() ) {
                                    Float val = (Float) message.getArg(argOffset);
                                    transportHandler.sendMessage(transportHandler.obtainMessage(ArdourConstants.OSC_STRIP_FADER, stripIndex, (int)Math.round(val * 1000)));
                                }
                                else {
                                    if ( t==null )
										Log.d(TAG, "fader change missed" + stripIndex.toString() + "\n");
                                }
                                break;
                            case "recenable":
//                                    System.out.printf("OSC rec changed on %d\n", stripIndex);

                                t = getTrack(stripIndex);
                                if (t!=null) {
                                    t.recEnabled = ((Float) message.getArg(argOffset) > 0);
                                    transportHandler.sendMessage(transportHandler.obtainMessage(ArdourConstants.OSC_STRIP_REC, stripIndex, 0));
                                }
                                else {
                                    Log.d(TAG, "recEnable missed\n");
                                }
                                break;
                            case "mute":
                                t = getTrack(stripIndex);
                                if (t!=null) {
                                    t.muteEnabled = ((Float) message.getArg(argOffset) > 0);
                                    transportHandler.sendMessage(transportHandler.obtainMessage(ArdourConstants.OSC_STRIP_MUTE, stripIndex, 0));
                                }
                                break;
                            case "solo":
                                t = getTrack(stripIndex);
                                if (t == null || t.type == null) {
                                    Log.d(TAG, "solo");
                                    break;
                                }
                                if (t!=null) {
                                    t.soloEnabled = ((Float) message.getArg(argOffset) > 0);
                                    transportHandler.sendMessage(transportHandler.obtainMessage(ArdourConstants.OSC_STRIP_SOLO, stripIndex, 0));
                                }
                                break;

                            case "solo_iso":
                                t = getTrack(stripIndex);
                                if (t!=null) {

                                    t.soloIsolateEnabled = ((Float) message.getArg(argOffset) > 0);
                                    transportHandler.sendMessage(transportHandler.obtainMessage(ArdourConstants.OSC_STRIP_SOLOISO, stripIndex, 0));
                                }
                                break;

                            case "solo_safe":
                                t = getTrack(stripIndex);
                                if (t!=null) {

                                    t.soloSafeEnabled = ((Float) message.getArg(argOffset) > 0);
                                    transportHandler.sendMessage(transportHandler.obtainMessage(ArdourConstants.OSC_STRIP_SOLOSAFE, stripIndex, 0));
                                }
                                break;

                            case "pan_stereo_position":
                                t = getTrack(stripIndex);
                                if (t!=null && !t.getTrackVolumeOnSeekBar() ) {
                                    t.panPosition = (Float) message.getArg(argOffset);
                                    transportHandler.sendMessage(transportHandler.obtainMessage(ArdourConstants.OSC_STRIP_PAN, stripIndex, 0));
                                }
                                break;

                            case "monitor_input":
                                t = getTrack(stripIndex);
                                if( t != null ) {
                                    if( message.getArg(argOffset) instanceof Integer) {
                                        t.stripIn = (int) message.getArg(argOffset) > 0;
                                        transportHandler.sendMessage(transportHandler.obtainMessage(ArdourConstants.OSC_STRIP_INPUT, stripIndex, 0));
                                    }
                                }
                                break;

                            case "select":
                                transportHandler.sendMessage(transportHandler.obtainMessage(
                                        ArdourConstants.OSC_STRIP_SELECT, (int)message.getArg(0), (float)message.getArg(1) > 0 ? 1 : 0));
                                break;

                            default:

//								System.out.printf("path: %s, ", message.getName());
//								for( int a = 0; a < message.getArgCount(); a++) {
//									System.out.printf("%d-%s,  ", a, String.valueOf(message.getArg(a)));
//								}
//								System.out.printf("\n");
                                break;
                        }
                        break;
                    case "position":
                        switch (pathes[2]) {
                            case "samples":
                                transportHandler.sendMessage(transportHandler.obtainMessage(ArdourConstants.OSC_UPDATE_CLOCK, Long.parseLong((String) message.getArg(0))));
                                break;
                            case "smpte":
                                transportHandler.sendMessage(transportHandler.obtainMessage(ArdourConstants.OSC_UPDATE_CLOCKSTRING, (String) message.getArg(0)));
                                break;
                        }
                        break;
                    case "master":
                        switch (pathes[2]) {

                            case "meter":
                                t = getTrack(masterId);

                                int newMeter = ((int)message.getArg(0)) & 0xffff;
                                newMeter = (newMeter != 0xffff) ? newMeter & 0x1FFF : 0;

                                if( t != null && t.meter != newMeter ) {
                                    t.meter = newMeter;
                                    transportHandler.sendMessage(transportHandler.obtainMessage(ArdourConstants.OSC_STRIP_METER, masterId, 0));
                                }
								else
									if( t == null )
										Log.d(TAG, "master meter change missed " + masterId.toString() + "\n");
                                break;

                            case "fader":
                                t = getTrack(masterId);
                                if ( t!=null && !t.getTrackVolumeOnSeekBar() ) {
                                    transportHandler.sendMessage(transportHandler.obtainMessage(ArdourConstants.OSC_STRIP_FADER,
                                            masterId, Math.round((Float)message.getArg(0) * 1000)));
                                }
                                else {
                                    if ( t==null )
                                        Log.d(TAG, "master fader change missed " + masterId.toString() + "\n");
                                }
                                break;

                            case "mute":
                                t = getMaster();
                                if (t!=null) {
									t.muteEnabled = ((Float) message.getArg(0) > 0);
                                    transportHandler.sendMessage(transportHandler.obtainMessage(ArdourConstants.OSC_STRIP_MUTE, masterId,  ((Float)message.getArg(0)).intValue()));
                                }
								else
									Log.d(TAG, "master mute change missed " + masterId.toString() + "\n");
								break;

                            case "name":
                                t = getMaster();
                                if (t!=null) {
                                    t.name = (String) message.getArg(0);
                                    transportHandler.sendMessage(transportHandler.obtainMessage(ArdourConstants.OSC_STRIP_NAME, masterId, 0));
                                }
								else
									Log.d(TAG, "master name change missed " + masterId.toString() + "\n");
                                break;
                            default:
                                break;
                        }
                        break;
                }
            }
            msg_count--;

        }
    };

    public int msg_count = 0;
    public int msg_max = 0;
    public boolean dropPackages = false;
    /**
	 * The OSC reply listener handler
	 */
	private OSCListener replyListener = new OSCListener(){
		
		@Override
		public void  messageReceived(OSCMessage message, SocketAddress addr, long time) {

            if(message.getName().startsWith("/strip/fader")) {
                System.out.printf("path: %s, ", message.getName());
                for (int a = 0; a < message.getArgCount(); a++) {
                    System.out.printf("%d-%s,  ", a, String.valueOf(message.getArg(a)));
                }
                System.out.printf("\n");
            }
            Message queueMessage = queueHandler.obtainMessage(999, message);
            if(!dropPackages)
                queueHandler.sendMessage(queueMessage);

		}
	};

    private Track getMaster() {
        return getTrack(masterId);
    }


    Track getTrack(int remoteId) {
        return routes.get(remoteId);
    }


	public void requestSends(int trackIndex) {
		Object[] args  = new Object[1];
		args[0] = trackIndex;
		this.sendOSCMessage("/strip/sends", args);
	}

	public void requestReceives(int trackIndex) {
		Object[] args  = new Object[1];
		args[0] = trackIndex;
		this.sendOSCMessage("/strip/receives", args);
	}

	/*
	 * Class properties getters and setters
	 */
	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}
	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	public void setSendEnable(int iAuxLayout, int source_id, float v) {
		Object[] args  = new Object[3];
		args[0] = iAuxLayout;
		args[1] = source_id;
		args[2] = v;
		this.sendOSCMessage("/strip/send/enable", args);
	}

	public void setSelectSendEnable(int trackIndex, int pluginIndex, float value) {
		Object[] args  = new Object[2];
		args[0] = pluginIndex + 1;
		args[1] = value;
		this.sendOSCMessage("/select/send_enable", args);
	}

    public HashMap<Integer, Track> getRoutes() {
		// TODO: change to hashMap implementation
        return routes;
    }

	public void requestPlugin(int trackIndex, int pluginIndex) {

		Object[] args = new Object[2];
		args[0] = trackIndex;
		args[1] = pluginIndex;

		this.sendOSCMessage("/strip/plugin/descriptor", args);

	}

	public void requestPluginList(int trackIndex) {

		Object[] args = new Object[1];
		args[0] = trackIndex;

		this.sendOSCMessage("/strip/plugin/list", args);

	}

	public void pluginFaderAction(Track pluginTrack, int pid, int ppid, double val) {
		Object[] args = new Object[4];
		args[0] = pluginTrack.remoteId;
		args[1] = pid;
		args[2] = ppid;
		args[3] = val;

		this.sendOSCMessage("/strip/plugin/parameter", args);
	}

	public void pluginAction(int command, Track pluginTrack, int pid) {
		switch(command) {
			case PLUGIN_RESET:
				Object[] args = new Object[2];
				args[0] = pluginTrack.remoteId;
				args[1] = pid;
				this.sendOSCMessage("/strip/plugin/reset", args);
				break;
		}
	}

	public void trackSendAction(int cmd, Track sendTrack, int sendIndex, int val) {
		switch (cmd) {
			case SEND_CHANGED:
				Object[] args = new Object[3];
				args[0] = sendTrack.remoteId;
				args[1] = sendIndex;
				args[2] = (float) val / 1000;
				sendOSCMessage("/strip/send/fader", args);
				break;
			case SEND_ENABLED:
				Object[] eargs  = new Object[3];
				eargs[0] = sendTrack.remoteId;
				eargs[1] = sendIndex;
				eargs[2] = (float)val;
				this.sendOSCMessage("/strip/send/enable", eargs);
				break;

		}
	}

	public void getProcessors(int iStripIndex) {
		Object[] eargs  = new Object[1];
		eargs[0] = iStripIndex;
		this.sendOSCMessage("/strip/processors", eargs);
	}

	public void selectStrip(int iStripIndex, boolean state) {
		Object[] eargs  = new Object[2];

		eargs[0] = iStripIndex;
		eargs[1] = state ? 1 : 0;
		this.sendOSCMessage("/strip/select", eargs);
	}

	public void pluginEnable(Track pluginTrack, int pluginId, boolean enabled) {
		Object[] eargs  = new Object[2];

		eargs[0] = pluginTrack.remoteId;
		eargs[1] = pluginId;
		this.sendOSCMessage(enabled ? "/strip/plugin/activate" : "/strip/plugin/deactivate", eargs);
	}


    public int getMasterId() {
        return masterId;
    }
}
