// https://github.com/hahyun8587/SimpleChat

import java.net.*;
import java.io.*;
import java.util.*;
import java.text.SimpleDateFormat;

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
			ArrayList<String> ban = new ArrayList<String>();

			while(true){
				Socket sock = server.accept();
				ChatThread chatthread = new ChatThread(sock, hm, ban);
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
	private ArrayList<String> ban;
	private boolean initFlag = false;
	private int badCount; 

	public ChatThread(Socket sock, HashMap<String, PrintWriter> hm, ArrayList<String> ban){
		this.sock = sock;
		this.hm = hm;
		this.ban = ban;

		try {
			String time = currTime();
			
			pw = new PrintWriter(new OutputStreamWriter(sock.getOutputStream()));
			br = new BufferedReader(new InputStreamReader(sock.getInputStream()));
			id = br.readLine();
			
			broadcast(id + " entered.", time);
			System.out.println(time + "[Server] User (" + id + ") entered.");
			
			synchronized(hm){
				hm.put(this.id, pw);
			}
			initFlag = true;
			badCount = 0;
		} catch (Exception ex) {
			System.out.println(ex);
		}
	} // construcor

	public void run(){
		try{
			String line = null;
			String bad = null;

			while((line = br.readLine()) != null) {
				String time = currTime();
				
				if ((bad = check(line)) != null)
					displayWarning(bad, time);	
				else {	

					if(line.equals("/quit"))
						break;
					else if (line.indexOf("/addspam") == 0)
						addSpam(line, time);
					else if (line.equals("/spamlist"))
						displayBan(time);	
					else if (line.equals("/userlist"))
						send_userlist(time);	
					else if(line.indexOf("/to ") == 0)
						sendmsg(line, time);
					else
						broadcast(id + " : " + line, time);
				}
			}
		} catch (Exception ex) {
			System.out.println(ex);
		} finally {
			synchronized(hm) {
				hm.remove(id);
			}
			String time = currTime();
			
			broadcast(id + " exited.", time);
			
			try {
				if(sock != null)
					sock.close();
			} catch (Exception ex){}
		}
	} // run

	public String currTime() 
	{
		Date time = new Date();
		SimpleDateFormat tForm = new SimpleDateFormat("hh:mm:ss");
		
		return "[" + tForm.format(time) + "]"; 
	}

	public void addSpam(String line, String time) 
	{
		synchronized (ban) {
			int start = line.indexOf(" ") + 1;

			if (start != 0) {
				String banWord = line.substring(start);

				ban.add(banWord);

				pw.printf("%s word %s added to the ban list.\n", time, banWord);
				pw.flush();
			}
		}
	}

	public void displayBan(String time)
	{	
		pw.printf("%s Banned Words\n", time);
		
		for (int i = 0; i < ban.size(); i++) 
			pw.printf("%d.%s\n", i + 1, ban.get(i));
		
		pw.flush();
	}

	// return banned word if the message contains banned word
	// get message from the user and reads the String array already initalized 
	// returns String if the message contains certain string and returns null if it doesn't  
	public String check(String msg)
	{	
		for (String str : ban) {
			if (msg.indexOf(str) != -1) {
				badCount++;

				return str;
			}
		}
		return null;
	}
	
	// get banned string and print warnings to user
	public void displayWarning(String str, String time)
	{	
		pw.printf("%s [WARNING] You can't use word %s.\n", time, str);
		pw.printf("%s [WARNING] You used bad language %d times.\n", time, badCount);
		pw.printf("%s [WARNING] You will get penalty if you use bad language for 5 times.", time); // needs update
		pw.flush();
	}

	// Print user's id whose accessing in the server 
	// reads iterator that has keys of the hashmap and print all of them to own socketstream 
	public void send_userlist(String time)
	{	
		synchronized(hm) {
			Set<String> set = hm.keySet();
			Iterator<String> iter = set.iterator();
			int count = 0;

			pw.printf("%s Userlist [%d users]\n", time, hm.size());

			while (iter.hasNext()) {
				count++;

				pw.printf("%d. ", count);
				pw.println(iter.next());
				pw.flush();
			}
		}
	}

	public void sendmsg(String msg, String time){
		int start = msg.indexOf(" ") +1;
		int end = msg.indexOf(" ", start);
		
		if (end != -1) {
			String to = msg.substring(start, end);

			if (to.equals(id)) {
				pw.printf("%s You can't whisper to yourself.", time);
				pw.flush();
			}
			else {
				String msg2 = msg.substring(end + 1);
				PrintWriter pwOther = null;
				
				synchronized(hm) { 
					pwOther = hm.get(to);
				}

				if(pwOther != null) {
					pwOther.println(time + " " + id + " whisphered. : " + msg2);
					pwOther.flush();
				} // if
			} // else
		}
	} // sendmsg

	public void broadcast(String msg, String time){
		synchronized(hm) {
			Collection<PrintWriter> collection = hm.values();
			Iterator<PrintWriter> iter = collection.iterator();
			
			while (iter.hasNext()) {
				PrintWriter pwOther= iter.next();

				if (pwOther != pw) {
					pwOther.println(time + msg);
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
