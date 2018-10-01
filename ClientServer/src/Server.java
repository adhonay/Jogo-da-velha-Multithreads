
import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server extends JFrame{

    public static final String MESSAGE_WAIT_OTHER_PLAYER = "waiting for the other player";
    public static final String MESSAGE_TURN_TO_PLAY = "turn to play";
    public static final String MESSAGE_GAME_IS_OVER = "the game is over";
    public static final String MESSAGE_INVALID_MOVE = "invalid movement";
    public static final String MESSAGE_OPPONENT_MOVED = "opponent moved";

    //GUI
    private static JTextArea display;
    private Dimension displayDimension;

    //SERVER
    private ServerSocket server;
    public static final int PORT_NUMBER = 1025;
    private final int PLAYER_MAX = 50;

    //FRONT END
    private ExecutorService frontEnd;

    //CLIENT THREAD
    private List<Player> playerList;
    private int playersCount = 0;
    private static final int MAX_PLAYERS = 2;



    //GAME
    private TicTacToe game = null;

    public Server(){

        super("Server");

        display = new JTextArea();
        displayDimension = new Dimension(400,300);

        playerList = new ArrayList<>();

        add( new JScrollPane(display), BorderLayout.CENTER );
        setSize(displayDimension);
        setVisible(true);

    }

    public void run(){

        try{

            //inicializa servidor
            server = new ServerSocket(PORT_NUMBER,PLAYER_MAX);

            //inicializa front end
            frontEnd = Executors.newFixedThreadPool(PLAYER_MAX);

            Player tempPlayer = null;

            while (true){

                displayMessage("Waiting for connection\n");

                if( playersCount % MAX_PLAYERS == 0 )
                    game = new TicTacToe();

                tempPlayer = new Player(server.accept(),this.playersCount,game);
                game.setPlayers( tempPlayer, playersCount % MAX_PLAYERS );
                playerList.add( tempPlayer );
                frontEnd.execute( tempPlayer );

                ++this.playersCount;
            }


        }catch (IOException ex){
            ex.printStackTrace();
        }
        finally {
            serverClose();
        }
    }

    private static void displayMessage( final String msg ){

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {

                display.append(msg);

            }
        });

    }

    private void serverClose(){

        try{
            server.close();
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    public static void main(String [] args ){

        Server server = new Server();
        server.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        server.run();

    }

    /*

        Classe Player(Thread)

     */
    private class Player implements Runnable{

        private Socket connection;
        private TicTacToe game;
        private int idPlayer;
        private Scanner input;
        private Formatter output;
        private int plays;

        public Player( Socket socket, int idPlayer, TicTacToe game ){

            this.connection = socket;
            this.idPlayer = idPlayer;
            this.game = game;

            //Obtém o fluxo de comunicação entre client e servidor
            try{

                getStreams();
                displayMessage("Player: " + idPlayer + " connected.\n");
                sendData("Player " + idPlayer + " online." );

                if( getMark() == 'X' )
                    game.setCurrentPlayer(this);

                sendData(getMark()+"");

            }catch (IOException ex){
                ex.printStackTrace();
            }
        }

        public char getMark(){
            return (idPlayer % MAX_PLAYERS == 0 ? 'X' : 'O' );
        }

        private void getStreams() throws IOException{

            this.output = new Formatter( connection.getOutputStream() );
            this.output.flush();
            this.input = new Scanner( connection.getInputStream() );

        }

        private String getData(){
            return input.nextLine();
        }

        private void sendData( String message ){

            output.format("%s\n",message);
            output.flush();
            displayMessage(String.format("%s\n",message));

        }

        @Override
        public void run() {

            game.waitForAnotherPlayer();

            do{

                game.play( this );

            }while( ! game.isOver() );

            sendData(MESSAGE_GAME_IS_OVER);

            if( game.winner != null ){

                if( game.winner == this ){
                    sendData("YOU WIN!");
                }
                else{
                    sendData("YOU LOST!");
                }

            }else {

                sendData("A TIE");

            }

        }

        private void closeConnection(){

            try{

                this.input.close();
                this.output.close();
                this.connection.close();

            }catch (IOException ex){
                ex.printStackTrace();

            }
        }
    }

    private class TicTacToe{

        private static final int PLAYER_X = 0;
        private static final int PLAYER_O = 1;
        private int numPlayers = 0;
        private char board[][];
        private Player currentPlayer = null;
        private Player[] players;
        private int lastPlayed = PLAYER_X;
        private Player winner = null;
        private boolean gameIsOver = false;

        public TicTacToe(){
            this.players = new Player[MAX_PLAYERS];
            this.board = new char[3][3];
            fillBoard();
        }

        private boolean checkLinesAndColumnsBoard(){

            int i = 0;
            while ( i < 3 ){

                if( (this.board[i][0] == players[lastPlayed].getMark() &&
                     this.board[i][1] == players[lastPlayed].getMark() &&
                     this.board[i][2] == players[lastPlayed].getMark() ) ||
                    (this.board[0][i] == players[lastPlayed].getMark()) &&
                     this.board[1][i] == players[lastPlayed].getMark() &&
                     this.board[2][i] == players[lastPlayed].getMark() ) {
                        return true;
                }

                ++i;
            }

            return false;
        }

        private boolean checkDiagonalsBoard(){

            int makedPoints = 0;

            if( this.board[1][1] == players[lastPlayed].getMark() ){

                ++makedPoints;

                if( (this.board[0][0] == players[lastPlayed].getMark() && this.board[2][2] == players[lastPlayed].getMark() ) ||
                     this.board[0][2] == players[lastPlayed].getMark() && this.board[2][0] == players[lastPlayed].getMark())
                    makedPoints += 2;
            }

            return ( makedPoints == 3 );
        }


        public synchronized boolean isOver() {

            displayMessage("Total turns: "+ (players[PLAYER_X].plays + players[PLAYER_O].plays) +"\n");

            if( ! gameIsOver ){

                if( players[PLAYER_X].plays < 3 )
                    return false;

                if( checkLinesAndColumnsBoard() || checkDiagonalsBoard() ){
                    winner = players[lastPlayed];
                    gameIsOver = true;
                    return true;
                }else if( (players[PLAYER_X].plays + players[PLAYER_O].plays) == 9  ){
                    gameIsOver = true;
                    return true;
                }else {
                    return false;
                }

            }else {

                return true;
            }
        }

        public synchronized void play( Player player ){

            String data = "";

            try{

                if( getCurrentPlayer() != player ){
                    //player.sendData(String.format("wait player %d play.\n",getOtherPlayer(player).idPlayer));
                    player.sendData(MESSAGE_WAIT_OTHER_PLAYER);
                    wait();

                }else {


                    player.sendData(MESSAGE_TURN_TO_PLAY);

                    data = player.getData();

                    displayMessage(data);

                    System.out.println( "charAt(0): "+ data.charAt(0) + "charAt(1): "+ data.charAt(1) + "\n" );


                    if( ! isValidateMove( data.charAt(0), Integer.parseInt( String.valueOf(data.charAt(1) ) ) ) ){
                        player.sendData(MESSAGE_INVALID_MOVE);
                        return;
                    }

                    player.plays++;

                    lastPlayed = ( player.getMark() == 'X' ? PLAYER_X : PLAYER_O );

                    getOtherPlayer(player).sendData(MESSAGE_OPPONENT_MOVED);
                    getOtherPlayer(player).sendData(data);

                    setCurrentPlayer(getOtherPlayer(currentPlayer));

                    displayBoard();

                    notifyAll();

                }


            }catch (InterruptedException e){
                e.printStackTrace();
            }

        }

        public void fillBoard(){

            for( int i = 0; i < 3 ;++i )
                for( int j = 0 ; j < 3; ++j )
                    this.board[i][j] = '-';

        }

        private void displayBoard(){

            displayMessage("\n");

            for( int i = 0; i < 3 ;++i ){
                for( int j = 0 ; j < 3; ++j )
                    displayMessage(this.board[i][j] + " ");
                displayMessage("\n");

            }
        }

        private boolean isValidateMove( char player, int position ){

            int lin = position/3;
            int col = position%3;

            System.out.printf( "lin: %d col: %d\n",lin,col );

            if( this.board[lin][col] == 'X' || this.board[lin][col] == 'O' )
                return false;

            this.board[lin][col] = player;

            return true;

        }

        public synchronized void waitForAnotherPlayer() {

            try {

                if (numPlayers != 2) {
                    getCurrentPlayer().sendData("waiting for another player...\n");
                    wait();
                }

                notifyAll();

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        public Player[] getPlayers() {
            return players;
        }

        public void setPlayers(Player player, int position ) {
            ++numPlayers;
            this.players[position] = player;
        }

        public Player getCurrentPlayer() {
            return currentPlayer;
        }

        public void setCurrentPlayer(Player currentPlayer) {
            this.currentPlayer = currentPlayer;
        }

        private Player getOtherPlayer( Player player ){
            if( players[PLAYER_X] == player )
                return players[PLAYER_O];
            else
                return players[PLAYER_X];
        }
    }

}
