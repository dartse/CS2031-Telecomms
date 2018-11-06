import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.Arrays;

public class Sender extends Thread {
	public static final int DEF_WINDOW_WIDTH = 5;// window
	private final int WINDOW_MAX = (DEF_WINDOW_WIDTH * 2);// whole range
	int srcPort; // where packets are sent from
	DatagramSocket srcSocket; // ^^
	int tgtPort;// target port
	String tgtName;// target name
	InetSocketAddress tgtAddr;// target address
	public Frame[] frameArray = new Frame[WINDOW_MAX];// array of active frames

	public Sender(String tgtName, int tgtPort, int srcPort) {
		try {
			this.srcPort = srcPort;
			srcSocket = new DatagramSocket(srcPort);
			this.tgtPort = tgtPort;
			this.tgtName = tgtName;
			tgtAddr = new InetSocketAddress(this.tgtName, this.tgtPort); // get an address
		} catch (SocketException e) {
			e.printStackTrace();
		}
	}

	private int activeFrames = 0;// number of frames currently active

	public boolean sendData(String data, String dstNode, int dstPort, int srcPort) {
		byte[][] stringsToSend = splitStr(data);// split the string into a series of strings
		int packetsToSend = stringsToSend.length; // number of packets to send
		byte pacNum = 0;
		int packetsSent = 0;
		while (packetsToSend > 0) { // while there are still packets to send
			while (activeFrames < DEF_WINDOW_WIDTH) {// while there is space in the window
				sendPacket(new Packet(tgtAddr, Packet.DATA, pacNum, stringsToSend[packetsSent]));// send it!
				pacNum = ((pacNum + 1) > WINDOW_MAX) ? 0 : pacNum++;// if is the last in the range set to 0 ie loop
																	// around window range
				packetsToSend--; // one packet sent
				packetsSent++;// ^^
			}
		}
		return false;
	}

	private void sendPacket(Packet thePack) {// send a packet
		frameArray[thePack.seqNum] = new Frame(thePack, srcSocket);// new frame in array
		frameArray[thePack.seqNum].send();// send it!
		activeFrames++;// frame is now active
	}

	public void ackRecieved(int index) { // ack-packet received
		if (index < frameArray.length && index >= 0) {// is in range of array
			frameArray[index].cancel(); // cancel frame timeout timer
			frameArray[index] = null; // nullify frame
			activeFrames--;// frame is no longer active
		}
	}

	public void nakRecieved(int index) { // nak-packet received
		if (index < frameArray.length && index >= 0) {// is in range of array
			frameArray[index].resend();// resend that sucker!
		}
	}

	private byte[][] splitStr(String theString) {// split a string into a series of byte arrays
		byte[] strByteArr = theString.getBytes(Packet.DEF_ENCODING);// string -> byte[]
		int noOfStrings = strByteArr.length / Packet.MAX_LENGTH_BYTES;// number of byte[] required
		if (strByteArr.length % Packet.MAX_LENGTH_BYTES != 0)// check for a shorter byte[] on the end
			noOfStrings++;
		byte[][] resByte = new byte[noOfStrings][];// new byte array array
		int start = 0;
		// if the length of the array is less than the max, use it as the max
		int end = (strByteArr.length < Packet.MAX_LENGTH_BYTES) ? strByteArr.length : Packet.MAX_LENGTH_BYTES;
		for (byte[] rsString : resByte) {// for every byte array
			rsString = Arrays.copyOfRange(strByteArr, start, end);// copy section of byte[]
			start += Packet.MAX_LENGTH_BYTES;
			end = ((end + Packet.MAX_LENGTH_BYTES) > theString.length()) ? theString.length()
					: (end + Packet.MAX_LENGTH_BYTES);
		}
		return resByte;
	}
}
