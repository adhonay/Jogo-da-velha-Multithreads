import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Formatter;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Client extends JFrame implements Runnable {

    //GUI
    private JTextArea display;
    private JTextField keyboard;
    private JLabel statusText;
    private Dimension displayDimension = new Dimension(400,300);
    private static final int NUM_CLIENTS = 6;
    private String host;
    private Socket serverConnection;
    private Formatter output;
    private Scanner input;
    private char clientMark;
    private Random random = new Random();
    private ExecutorService worker;

    public Client( String host ){

        super("Client");
        this.host = host;

        this.display = new JTextArea();
        this.keyboard = new JTextField();
        this.keyboard.setEditable(true);
        this.keyboard.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                output.format(e.getActionCommand());
                displayMessage(e.getActionCommand());
                output.flush();
            }
        });

        this.statusText = new JLabel();

        add(keyboard, BorderLayout.NORTH );
        add( new JScrollPane(display), BorderLayout.CENTER );
        add( statusText, BorderLayout.SOUTH );

        setSize(displayDimension);
        setVisible(true);

        startClient();

    }

    private int generatePosition(){
        return Math.abs( random.nextInt() % 9 );
    }

    public void startClient(){

        try {

            connectionToServer();
            worker = Executors.newFixedThreadPool(1);
            worker.execute(this);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run(){

        try{

            getStreams();
            processConnection();

        }catch (UnknownHostException ex){
            ex.printStackTrace();
        }catch (IOException ex){
            ex.printStackTrace();
        }
        finally {
            closeConnection();
        }
    }

    private void sendData( String message ){

        output.format("%s\n",message);
        output.flush();
        displayMessage(String.format("%s\n",message));

    }

    private void processConnection() throws IOException {

        try {

            try {
                Thread.sleep(generatePosition()*1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            displayMessage(getData()+"\n");
            clientMark = getData().charAt(0);
            displayMessage("Symbol: "+clientMark+"\n");

            String message = getData();

            while ( ! message.equals(Server.MESSAGE_GAME_IS_OVER) ){
                processMessage(message);
                message = getData();
            }

            displayMessage(getData());

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void processMessage(String message) throws IOException, ClassNotFoundException{

        if( message.equals(Server.MESSAGE_TURN_TO_PLAY) ){
            sendData(clientMark+""+generatePosition());
        }
        else if( message.equals(Server.MESSAGE_INVALID_MOVE) ){
            sendData(clientMark+""+generatePosition());
        }else if( message.equals(Server.MESSAGE_OPPONENT_MOVED) ){

            String data = getData();

            char opponentMark = data.charAt(0);
            int position =  Integer.parseInt( data.charAt(1) + "" );

            int lin = position/3;
            int col = position%3;

            displayMessage(String.format("Opponent moved: { lin: %d col: %d mark: %c }\n",lin,col,opponentMark));

        }

    }

    private String getData() throws IOException, ClassNotFoundException {
        return input.nextLine();
    }

    private void connectionToServer()throws UnknownHostException, IOException{

        serverConnection = new Socket( InetAddress.getByName(host), Server.PORT_NUMBER );

        displayMessage( "Connected to: " + serverConnection.getInetAddress().getHostName() + "\n" );

    }

    private void getStreams() throws IOException{

        this.output = new Formatter( serverConnection.getOutputStream() );
        this.output.flush();
        this.input = new Scanner( serverConnection.getInputStream() );
        displayMessage("Got I/O streams.\n");

    }

    private void displayMessage( final String msg ){

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {

                display.append(msg);

            }
        });
    }

    private void closeConnection(){

        displayMessage("\n Terminating connection\n");

        try{

            output.close();
            input.close();
            serverConnection.close();

        }catch (IOException ex){
            ex.printStackTrace();
        }

    }

    public static void main(String [] args){

        Client clients[] = new Client[NUM_CLIENTS];

        for( int i = 0 ; i < clients.length ; ++i ){
            clients[i] = new Client("127.0.0.1" );
            clients[i].setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        }


    }
}
