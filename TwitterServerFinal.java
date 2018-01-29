import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import javax.swing.JOptionPane;
import java.io.PrintWriter;
import java.util.HashSet;
import java.net.InetAddress;
import java.net.UnknownHostException;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.JFrame;
import javax.swing.JTextField;

public class TwitterServerFinal {

    private static final int PORT = 9001;
    private static HashSet<String> ids = new HashSet<String>();
    private static HashSet<PrintWriter> writers = new HashSet<PrintWriter>();

    public static void main(String[] args) throws Exception 
    {
        ServerSocket listener = new ServerSocket(PORT);
        InetAddress ip;
        try {
                ip = InetAddress.getLocalHost();
                System.out.println("Current IP address : " + ip.getHostAddress());
            } 
            catch (UnknownHostException e) 
            {
                e.printStackTrace();
            }
        try 
        {
            while (true) 
            {
                Handler h = new Handler(listener.accept());
                h.start();
                TwitterClient t = new TwitterClient();
                t.start();
                
            }
        } 
        finally 
        {
            listener.close();
        }
    }

    private static class Handler extends Thread 
    {
        private String id;
        private BufferedReader in;
        private PrintWriter out;
        private Socket socket;
        
        public Handler(Socket socket) 
        {
            this.socket = socket;
        }

        public void run() 
        {
            try 
            {
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                while(true) 
                {
                    out.println("SUBMITID");
                    id = in.readLine();
                    if (id == null) 
                    {
                        return;
                    }
                    synchronized (ids) 
                    {
                        if (!ids.contains(id)) 
                        {
                            ids.add(id);
                            break;
                        }
                    }
                }
                out.println("IDACCEPTED");
                writers.add(out);

                while(true) 
                {
                    String input = in.readLine();
                    if (input != null) 
                    {
                        for (PrintWriter writer : writers) 
                        {
                            writer.println("Twitter " + id + ": " + input); 
                            //currently just prints but could also send to logs
                            //of desinated clients
                        }
                    }
                    else
                    {
                        return;
                    }                   
                }
            } 
            catch (IOException e) 
            {
                System.out.println(e);
            } 
            finally 
            {
                if (out != null) 
                {
                    writers.remove(out);
                }
                if (id != null) 
                {
                    ids.remove(id);
                }
                try 
                {
                    socket.close();
                } 
                catch (IOException e)
                {

                }
            }
        }
    }

    private static class TwitterClient extends Thread
    {

        BufferedReader in;
        PrintWriter out;
        JFrame frame = new JFrame("Twitter");
        JTextField textField = new JTextField(40);
        JTextArea TwitterArea = new JTextArea(8, 40);

        public void main(String[] args) throws Exception 
        {
            
            TwitterClient client = new TwitterClient();
            client.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            client.frame.setVisible(true);
            client.run();

        }
        
        public TwitterClient() 
        {

            frame.getContentPane().add(new JScrollPane(TwitterArea), "Center");
            frame.getContentPane().add(textField, "South");
            textField.setEditable(false);
            TwitterArea.setEditable(false);
            frame.pack();

            textField.addActionListener(new ActionListener() 
            {
     
                public void actionPerformed(ActionEvent e) 
                {
                    out.println(textField.getText());
                    textField.setText("");
                }
            });
        }

        public String getID() 
        {
            return JOptionPane.showInputDialog(
                frame,
                "ID:",
                "Virtual Machine ID:",
                JOptionPane.PLAIN_MESSAGE);
        }

        public String getServerAddress() 
        {
            return JOptionPane.showInputDialog(
                frame,
                "Enter IP Address of the Server:",
                "Twitter",
                JOptionPane.QUESTION_MESSAGE);
        }

        public void run()
        {

            String serverAddress = getServerAddress();
            try
            {
                Socket socket = new Socket(serverAddress, 9001);

                out = new PrintWriter(socket.getOutputStream(), true);
        
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                while(true) 
                {
                        String line = in.readLine();

                    if (line.startsWith("Twitter")) 
                    {
                        TwitterArea.append(line.substring(8) + "\n");
                    }
                    else if (line.startsWith("IDACCEPTED")) 
                    {
                        textField.setEditable(true);
                    } 
                    else if (line.startsWith("SUBMITID")) 
                    {
                        out.println(getID());
                    } 
                    else
                    {

                    } 
                }
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }    
}

