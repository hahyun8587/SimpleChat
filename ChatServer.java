// https://github.com/hahyun8587/SimpleChat

import java.net.*;
import java.io.*;
import java.util.*;

/** 
 * FOR UPDATE
 * clear
 * friends
 * login 
 * duplicate id check
 * server ending
*/
public class ChatServer {
	public static void main(String[] args) {
		try{
			ServerSocket server = new ServerSocket(10001);
			System.out.println("Waiting connection...");
			HashMap<String, PrintWriter> hm = new HashMap<String, PrintWriter>();
			while(true){
				Socket sock = server.accept();
				ChatThread chatthread = new ChatThread(sock, hm);
				chatthread.start();
			} // while
		}catch(Exception e){
			System.out.println(e);
		}
	} // main
}

class ChatThread extends Thread{
	private Socket sock;
	private String id;
	private PrintWriter pw;
	private BufferedReader br;
	private HashMap<String, PrintWriter> hm;
	private String[] badStr = {"badLang1", "badLang2", "badLang3", "badLang4", "badLang5"};
	private boolean initFlag = false;
	private int badCount; 

	public ChatThread(Socket sock, HashMap<String, PrintWriter> hm){
		this.sock = sock;
		this.hm = hm;
		try{
			pw = new PrintWriter(new OutputStreamWriter(sock.getOutputStream()));
			br = new BufferedReader(new InputStreamReader(sock.getInputStream()));
			id = br.readLine();
			broadcast(id + " entered.");
			System.out.println("[Server] User (" + id + ") entered.");
			synchronized(hm){
				hm.put(this.id, pw);
			}
			initFlag = true;
			badCount = 0;
		}catch(Exception ex){
			System.out.println(ex);
		}
	} // construcor

	public void run(){
		try{
			String line = null;

			while((line = br.readLine()) != null) {
				if (containsBadLang(line))
					displayWarning();	
				else {	
					if(line.equals("/quit"))
						break;
					else if (line.equals("/userlist"))
						send_userlist();
					else if(line.indexOf("/to ") == 0)
						sendmsg(line);
					else
						broadcast(id + " : " + line);
				}
			}
		} catch (Exception ex) {
			System.out.println(ex);
		} finally {
			synchronized(hm) {
				hm.remove(id);
			}
			broadcast(id + " exited.");
			
			try {
				if(sock != null)
					sock.close();
			} catch (Exception ex){}
		}
	} // run

	// Checks if the message contains banned words
	// get string from the user and reads the String array already initalized 
	// returns true if the string contains certain string and returns false if it doesn't  
	public boolean containsBadLang(String msg)
	{	
		for (String str : badStr) {
			if (msg.indexOf(str) != -1) {
				badCount++;

				return true;
			}
		}
		return false;
	}
	
	// Print warnings to user
	public void displayWarning()
	{	
		pw.printf("[WARNING] You used bad language %d times.\n", badCount);
		pw.println("[WARNING] You will get penalty if you use bad language for 5 times."); // needs update
		pw.flush();
	}

	// Print user's id whose accessing in the server 
	// reads iterator that has keys of the hashmap and print all of them to own socketstream 
	public void send_userlist()
	{	
		synchronized(hm) {
			Set<String> set = hm.keySet();
			Iterator<String> iter = set.iterator();
			
			pw.printf("Userlist [%d users]\n", hm.size());

			while (iter.hasNext()) {
				pw.println(iter.next());
				pw.flush();
			}
		}
	}

	public void sendmsg(String msg){
		int start = msg.indexOf(" ") +1;
		int end = msg.indexOf(" ", start);
		
		if (end != -1) {
			String to = msg.substring(start, end);

			if (to.equals(id)) {
				pw.println("You can't whisper to yourself.");
				pw.flush();
			}
			else {
				String msg2 = msg.substring(end + 1);
				PrintWriter pwOther = null;
				
				synchronized(hm) { 
					pwOther = hm.get(to);
				}

				if(pwOther != null) {
					pwOther.println(id + " whisphered. : " + msg2);
					pwOther.flush();
				} // if
			} // else
		}
	} // sendmsg

	public void broadcast(String msg){
		synchronized(hm) {
			Collection<PrintWriter> collection = hm.values();
			Iterator<PrintWriter> iter = collection.iterator();
			
			while (iter.hasNext()) {
				PrintWriter pwOther= iter.next();

				if (pwOther != pw) {
					pwOther.println(msg);
					pwOther.flush();
				} // if 
			}
		}
	} // broadcast 

	// getter (needs update)
	public int getBadCount()
	{
		return badCount;
	}
}
