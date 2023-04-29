package com.example.chess;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;

import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.fonts.Font;
import android.media.Image;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.Timer;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private Handler handler;
    private int delay = 10;
    public String displaymessage;
    public String modemessage;

    MediaPlayer placed;
    MediaPlayer replaced;
    MediaPlayer check;
    MediaPlayer invalid;
    MediaPlayer gameover;

    private int piece;
    private int rowpix, colpix;
    private int clicked=0;

    private int search_depth=4;// the depth upto which the minimax search tree explores all possible moves for both the teams
//
    private int[][] board =new int [8][8];
    private long freezone=0;
    private long redzone=0;
    private long bluezone=0;
    private int promote_index=0;
    private int[][] promoted_piece ={{-5,-2,-3,-4},{5,2,3,4}};

    private int black_king_checked=0;
    private int white_king_checked=0;

    private boolean automatic=true;

    private boolean running=true;

    private double inf=99999999;//infinity

    Board b=null;
    int turn=-1;//-1 corresponds to white's turn and 1 corresponds to black's turn

    public void initialise()
    {
        board[0][0]=-2;//black Rook
        board[0][1]=-4;//black Horse
        board[0][2]=-3;//black Bishop
        board[0][3]=-5;//black Queen
        board[0][4]=-6;//black King
        board[0][5]=-3;//black Bishop
        board[0][6]=-4;//black Horse
        board[0][7]=-2;//black Rook
        for(int i=0;i<8;i++)
            board[1][i]=-1;//black pawn
        for(int i=0;i<8;i++)
            board[6][i]=1;//white pawn
        board[7][0]=2;//white Rook
        board[7][1]=4;//white Horse
        board[7][2]=3;//white Bishop
        board[7][3]=5;//white Queen
        board[7][4]=6;//white King
        board[7][5]=3;//white Bishop
        board[7][6]=4;//white Horse
        board[7][7]=2;//white Rook

        b=new Board();
        b.reset();
        setBoard();

        freezone=0;
        redzone=0;
        bluezone=0;
        promote_index=0;
        black_king_checked=0;
        white_king_checked=0;
        running=true;
        turn=-1;
        displaymessage = "WHITE'S TURN";
        modemessage = "USER VS COMPUTER";
        paint();
    }

    public void setBoard()
    {
        Log.d("ChessFlow", "setBoard: Board has been set with updates");
        int[] change = {-2,-4,-3,-5,-6,-3,-4,-2,-1,-1,-1,-1,-1,-1,-1,-1,1,1,1,1,1,1,1,1,2,4,3,5,6,3,4,2};
        for(int i=0;i<8;i++)
        {
            for(int j=0;j<8;j++)
            {
                board[i][j]=0;
            }
        }
        for(int i=0;i<32;i++)
        {
            if(b.piece[i]!=0)
            {
                int loc=Long.numberOfLeadingZeros(b.piece[i]);
                int x=loc/8;
                int y=loc%8;
                board[x][y]=change[b.designation[i]];
            }
        }
    }

    int piece_index(long x)//method returns the chess piece index located at location denoted by 1 in bitwise long x
    {
        int p=-1;
        for(int i=0;i<32;i++)
        {
            if((b.piece[i]&x)!=0)
            {
                p=i;
                break;
            }
        }
        return p;
    }

    Board appropriate_call(int j)//method for finding all the legal moves for a piece index j
    {
        Board temp=null;
        int depth=1;
        int choice;

        int i=b.designation[j];

        if(i<16)
            choice=1;
        else
            choice=-1;

        if(i==0 || i==7 || i==24 || i==31)
        {
            temp=b.rook(j,choice,depth,-inf,inf);
        }
        else if(i==1 || i==6 || i==25 || i==30)
        {
            temp=b.knight(j,choice,depth,-inf,inf);
        }
        else if(i==2 || i==5 || i==26 || i==29)
        {
            temp=b.bishop(j,choice,depth,-inf,inf);
        }
        else if(i==3 || i==27)
        {
            temp=b.queen(j,choice,depth,-inf,inf);
        }
        else if(i==4 || i==28)
        {
            temp=b.king(j,choice,depth,-1,-inf,inf);
        }
        else if(i>=8 && i<24)
        {
            temp=b.pawn(j,choice,depth,-inf,inf);
        }
        return temp;
    }

    void check()//method to find out if the black/white king is at check
    {
        Board temp=b.minimax(1,0,-inf,inf);
        if(temp!=null && temp.weight<(-b.king_value/2))//if white king would be eliminated if team black is to make a move
            white_king_checked=1;//then it means white king is already at check
        else
            white_king_checked=0;

        temp=b.minimax(-1,0,-inf,inf);
        if(temp!=null && temp.weight>(b.king_value/2))//if black king would be eliminated if team white is to make a move
            black_king_checked=1;//then it means black king is already at check
        else
            black_king_checked=0;
    }

    void users_turn(int lower ,int upper)//method to make a manual player's move
    {
        int row=rowpix;
        int col=colpix;
        long loc=1;
        loc<<=(63-(8*row+col));
        int index=piece_index(loc);


        if(clicked==1)//when a piece is selected
        {
            Log.d("ChessFlow", "users_turn: piece is selected");
            b.blackComponents();
            b.whiteComponents();
            redzone=b.whitePos|b.blackPos;// is the zone where one chess piece move would replace another chess piece
            freezone=redzone;//is the zone where a chess piece can legally move without collision
            bluezone=loc;//is the location of chess piece selected to be moved


            Board temp=null;

            if(index>=lower && index<=upper)
            {
                b.piece_moved=index;
                temp=appropriate_call(index);//find all legal moves for the selected chess piece
            }


            if(temp!=null && white_king_checked==1 && (index<16 || temp.weight<(-b.king_value/2)))//if its white's turn and a black
                temp=null;// piece is selected or white king is checked and this move eliminates white king, then this move is invalid
            else if(temp!=null && black_king_checked==1 && (index>=16 || temp.weight>(b.king_value/2)))//if its black's turn and a white
                temp=null;// piece is selected or black king is checked and this move eliminates black king, then this move is invalid

            if((turn==1 && index>=16) || (turn==-1 && index<16))
                temp=null;

            if(temp!=null)
            {
                redzone=temp.states&redzone;
                freezone=temp.states&(~redzone);
                if((redzone|freezone)==0)//if no legal moves for the selected piece is available
                    bluezone=0;//the piece is not selected
                clicked=2;
                paint();
            }
            else //if a wrong cell is selected which is either a blank cell or contains opponents chess piece
            {
                clicked=0;//then another piece has to be selected again
                check();
                if((white_king_checked==1 && turn==-1) || (black_king_checked==1 && turn==1))
                {//if the player's king is at check during this wrong selection, check alert sound is played
                    check.start();
                }
            }
        }
        else if(clicked==3)//when the cell to which the chess piece is to be moved is selected
        {
            Log.d("ChessFlow", "users_turn: piece placed");
            if((loc&(freezone|redzone))!=0)//if the selected cell corresponds to a legal chess move
            {
                b.piece[b.piece_moved]=loc;//the selected chess piece's location is updated
                if(index!=-1)
                    b.piece[index]=0;

                int des=b.designation[b.piece_moved];
                if(des>=8 && des<16 && (loc>>>8)==0)//if black pawn is at the last row
                {
                    promote_index=b.piece_moved;//then this piece is to be promoted
                    clicked=4;
                }
                else if(des>=16 && des<24 && (loc<<8)==0)//if white pawn is at the first row
                {
                    promote_index=b.piece_moved;//then this piece is to be promoted
                    clicked=4;
                }
                else
                {
                    promote_index=0;//for all other situations there is no promotion
                    if(automatic && turn==-1)//if the game is user vs computer and it is the black's turn now
                        clicked=6;//control transferred to the computer to make the next move
                    else
                        clicked=0;//else control is transferred to the other user player playing the game
                    setBoard();
                    check();
                    turn=-turn;

                    if((((b.minimax(turn,1,-inf,inf)).weight)*turn)>(b.king_value/2))//if checkmate condition is reached
                    {
                        running=false;//the game comes to an end
                        gameover.start();
                    }
                }

                b.whiteComponents();
                b.blackComponents();

                if((black_king_checked==1 && turn==1) || (white_king_checked==1 && turn==-1))//if either of king is at check
                {
                    check.start();//sound for check alert is played
                }
                else if(((redzone&b.whitePos)!=0 && turn==1) || ((redzone&b.blackPos)!=0 && turn==-1))//if a piece is replaced
                {
                    replaced.start();//sound for piece replacement is played
                }
                else
                {
                    placed.start();//sound for piece placed is played
                }

            }
            else if(index>=lower && index<=upper)//if some other chess piece is selected instead of the cell to which already
                clicked=1;// selected chess piece is to be moved
            else
            {
                clicked=0;//for wrong selection, the user needs to choose again
                check();
                if((white_king_checked==1 && turn==-1) || (black_king_checked==1 && turn==1))//if either king at check
                {
                    check.start();//sound for check alert is played
                }
                else
                {
                    invalid.start();//error sound is played denoting wrong cell selection
                }

            }

            redzone=0;
            freezone=0;
            bluezone=0;
            paint();
        }
        else if(clicked==5)//if a pawn has reached its end position and is to be promoted
        {
            if(row==3 && col==3)//this cell is selected to promote pawn to queen
            {
                int des=3;//black queen by default
                if(promote_index>=16)//if white's pawn is promoted
                    des=27;//then white queen
                b.designation[promote_index]=des;
                clicked=0;
            }
            else if(row==3 && col==4)//this cell is selected to promote pawn to rook
            {
                int des=0;//black rook by default
                if(promote_index>=16)//if white's pawn is promoted
                    des=24;//then white rook
                b.designation[promote_index]=des;
                clicked=0;
            }
            else if(row==4 && col==3)//this cell is selected to promote pawn to bishop
            {
                int des=2;//black bishop by default
                if(promote_index>=16)//if white's pawn is promoted
                    des=26;//then white bishop
                b.designation[promote_index]=des;
                clicked=0;
            }
            else if(row==4 && col==4)//this cell is selected to promote pawn to knight
            {
                int des=1;//black bishop by default
                if(promote_index>=16)//if white's pawn is promoted
                    des=25;//then white bishop
                b.designation[promote_index]=des;
                clicked=0;
            }
            else
                clicked=4;// if invalid cell is selected then the user needs to select promoted piece again

            if(clicked==0)//if appropriate cell is selected
            {
                if(automatic && turn==-1)//and it is user vs computer game
                    clicked=6;// the control is transferred to the computer to make the next move

                promote_index=0;
                setBoard();
                check();
                turn=-turn;

                if((((b.minimax(turn,1,-inf,inf)).weight)*turn)>(b.king_value/2))//if it result in a checkmate
                {
                    running=false;//the game comes to an end
                    gameover.start();
                }
                redzone=0;
                freezone=0;
                bluezone=0;
                paint();
            }
        }
    }

    public void getImageVal(int n)//method to acquire the appropriate chess piece image for painting the chess board
    {
        switch(n)
        {
            case -1:  piece = R.drawable.blackpawn;
                break;
            case -2:  piece = R.drawable.blackrook;
                break;
            case -3:  piece = R.drawable.blackbishop;
                break;
            case -4:  piece = R.drawable.blackknight;
                break;
            case -5:  piece = R.drawable.blackqueen;
                break;
            case -6:  piece = R.drawable.blackking;
                break;
            case 1:  piece = R.drawable.whitepawn;
                break;
            case 2:  piece = R.drawable.whiterook;
                break;
            case 3:  piece = R.drawable.whitebishop;
                break;
            case 4:  piece = R.drawable.whiteknight;
                break;
            case 5:  piece = R.drawable.whitequeen;
                break;
            case 6:  piece = R.drawable.whiteking;
                break;
        }
    }

    public void paint()
    {
        if(turn==-1)
            displaymessage = "WHITE'S TURN";
        else
            displaymessage = "BLACK'S TURN";

        if(automatic)
            modemessage = "USER VS COMPUTER";
        else
            modemessage = "USER VS USER";

        if(clicked!=2 && white_king_checked==1)//if white king is at check
            redzone=b.piece[28];//white king's cell is highlighted in red
        else if(clicked!=2 && black_king_checked==1)//if black king is at check
            redzone=b.piece[4];//black king's cell is highlighted in red


        String freestring=Long.toBinaryString(freezone);
        while(freestring.length()<64)
            freestring="0"+freestring;//string of bits with 1's denoting cells which are valid positions for selected chess piece
        String redstring=Long.toBinaryString(redzone);
        while(redstring.length()<64)
            redstring="0"+redstring;//string of bits with 1's denoting cells which are positions of collision for selected chess piece move
        String bluestring=Long.toBinaryString(bluezone);
        while(bluestring.length()<64)
            bluestring="0"+bluestring;//string of bits with 1's denoting cell where selected chess piece is located
        int alpha = 100; // 25% transparent
        int green = Color.argb(alpha, 0, 255, 0);//green color
        int red = Color.argb(alpha, 255, 0, 0);//red color
        int blue= Color.argb(alpha, 0, 0, 255);//blue color

        for(int i=0; i<64; i++)//painting appropriate chess icons in the chess cells
        {
            int row=i/8;
            int col=i%8;
            int x=board[row][col];
            String buttonName = "button" + row + col;
            getImageVal(x);
            ImageButton img = (ImageButton)findViewById(getResources().getIdentifier(buttonName,
                    "id", this.getPackageName()));
            img.setImageAlpha(255);
            if(freestring.charAt(i)=='1')
            {
                img.setBackgroundColor(green);
            }
            else if(redstring.charAt(i)=='1')
            {
                img.setBackgroundColor(red);
            }
            else if(bluestring.charAt(i)=='1')
            {
                img.setBackgroundColor(blue);
            }
            else
            {
                img.setBackgroundColor(Color.TRANSPARENT);
            }
            if(x!=0)
                img.setImageDrawable(ResourcesCompat.getDrawable(getResources(), piece, null));
            else
                img.setImageDrawable(ResourcesCompat.getDrawable(getResources(),
                        R.drawable.transparent, null));
        }

        if(promote_index!=0)//if a pawn is to be promoted
        {
            displaymessage = "SELECT REPLACEMENT FOR PROMOTION";
            int temp=1;
            if(promote_index<16)
                temp=0;
            int silver= Color.argb(200, 150, 150, 192);
            int gold = Color.argb(150, 150,150,55);
            for(int i=0; i<64; i++)//painting all the pieces to be chosen for promotion
            {
                int row = i / 8;
                int col = i % 8;
                int x = board[row][col];
                String buttonName = "button" + row + col;
                getImageVal(x);
                ImageButton img = (ImageButton) findViewById(getResources().getIdentifier(buttonName,
                        "id", this.getPackageName()));
                img.setBackgroundColor(gold);
                if (i==27) {
                    getImageVal(promoted_piece[temp][0]);
                    img.setImageDrawable(ResourcesCompat.getDrawable(getResources(), piece, null));
                }
                else if(i==28){
                    getImageVal(promoted_piece[temp][1]);
                    img.setImageDrawable(ResourcesCompat.getDrawable(getResources(), piece, null));
                }
                else if(i==35){
                    getImageVal(promoted_piece[temp][2]);
                    img.setImageDrawable(ResourcesCompat.getDrawable(getResources(), piece, null));
                }
                else if(i==36){
                    getImageVal(promoted_piece[temp][3]);
                    img.setImageDrawable(ResourcesCompat.getDrawable(getResources(), piece, null));
                }
                else {
                    img.setBackgroundColor(silver);
                    img.setImageAlpha(70);
                }
            }
        }
        if(!running)//if game comes to an end
        {
            modemessage = "GAME OVER";
            int silver= Color.argb(200, 150, 150, 192);
            int gold = Color.argb(150, 150,150,55);
            for(int i=0; i<64; i++)//painting all the pieces to be chosen for promotion
            {
                int row = i / 8;
                int col = i % 8;
                int x = board[row][col];
                String buttonName = "button" + row + col;
                getImageVal(x);
                ImageButton img = (ImageButton) findViewById(getResources().getIdentifier(buttonName,
                        "id", this.getPackageName()));
                if(redstring.charAt(i)=='0')
                    img.setBackgroundColor(gold);
                img.setImageAlpha(150);
            }

            if(black_king_checked==1)//if black king is checked
            {
                displaymessage = "Checkmate! White Wins";
            }
            else if(white_king_checked==1)//if white king is checked
            {
                displaymessage = "Checkmate! Black Wins";
            }
            else
            {
                displaymessage = "Game ends in a Draw";
            }
        }
        TextView tvmode = (TextView)findViewById(R.id.modelabel);
        tvmode.setText(modemessage);
        TextView tvdisplay = (TextView)findViewById(R.id.displaybar);
        tvdisplay.setText(displaymessage);
        Log.d("ChessFlow", "paint: existing board is painted");
    }

    public void performAction() {
        if(running)
        {
            Log.d("ChessFlow", "performAction: clicked="+clicked);
            if(clicked==1 || clicked==3 || clicked==5)//if it is user's turn or the game is player vs player
                users_turn(0,31);
            else if(clicked==6)//if computer has to make a move against the user
            {
                Log.d("ChessFlow", "performAction: Computer decides a move");
                Board temp=new Board(b.piece,b.designation);
                temp.blackComponents();
                temp.whiteComponents();
                redzone=temp.whitePos|temp.blackPos;

                Board find_legal=new Board(b.piece,b.designation);
                find_legal=find_legal.king(4, 1, 1, -1,-inf,inf);//find the best move the black king can make

                long legal_moves_king=0;
                if(find_legal!=null)//if black king has any legal moves to make
                    legal_moves_king=find_legal.states;//store all the legal moves possible for the black king

                temp.black_king_legal=legal_moves_king;//these legal states can only be choosen from for the black king

                temp=temp.minimax(1, search_depth,-inf,inf);//find the best chess move that black can take
                bluezone=b.piece[temp.piece_moved];
                b=temp;
                b.blackComponents();
                b.whiteComponents();

                freezone=b.states&(~redzone);
                redzone=b.states&(~freezone);
                paint();//highlight all the legal positions the selected black chess piece can make
                clicked=7;

            }
            else if(clicked==7)
            {
                Log.d("ChessFlow", "performAction: Computer pauses before making a move");
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }//wait for 0.5 seconds before making the move

                Log.d("ChessFlow", "performAction: Computer makes a move");
                setBoard();
                check();
                if(white_king_checked==1)//if the move places the white king in check
                {
                    check.start();//sound for check alert is played
                }
                else if((redzone&b.blackPos)!=0)
                {
                    replaced.start();//if the move replaces opponent's chess piece, sound for replaced is played
                }
                else
                {
                    placed.start();//sound for chess piece placed is played
                }

                turn=-turn;//white's turn now
                if((((b.minimax(turn,1,-inf,inf)).weight)*turn)>(b.king_value/2))//if there is a checkmate
                {
                    running=false;//game comes to an end
                    gameover.start();
                }
                redzone=0;
                freezone=0;
                bluezone=0;
                paint();
                clicked=0;
            }
        }
    }

    public void boardClicked(View v) {
        String s = getResources().getResourceName(v.getId());
        int l = s.length();
        int row = Integer.parseInt(s.substring(l-2,l-1));
        int col = Integer.parseInt(s.substring(l-1));
        if(running)
        {
            rowpix = row;
            colpix = col;
            if(clicked==0)
                clicked=1;//register mouse click when a chess piece is selected
            else if(clicked==2)
                clicked=3;//register mouse click when the cell to which the chess piece is to moved is selected
            else if(clicked==4)
                clicked=5;//register mouse click when the chess piece to which the pawn is to be promoted is selected
            Log.d("ChessFlow", " boardClicked: clicked="+clicked);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initialise();
        placed = MediaPlayer.create(this, R.raw.placed);
        replaced = MediaPlayer.create(this, R.raw.replaced);
        check = MediaPlayer.create(this, R.raw.check);
        invalid = MediaPlayer.create(this, R.raw.invalid);
        gameover = MediaPlayer.create(this, R.raw.gameover);

        handler = new Handler();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                    performAction();
                    handler.postDelayed(this, delay);
            }
        };
        handler.postDelayed(runnable, delay);
    }

    @Override
    protected void onStop() {
        super.onStop();
        placed.release();
        replaced.release();
        check.release();
        invalid.release();
        gameover.release();
    }

    @Override
    public void onClick(View v) {
        Log.d("ChessFlow", "Running status: "+running);
        switch(v.getId()) {
            case R.id.reset:
                initialise();
                break;
            case R.id.gamemode:
                automatic = !automatic;
                TextView tv = findViewById(R.id.modelabel);
                if(automatic) {
                    tv.setText("USER VS COMPUTER");
                }
                else{
                    tv.setText("USER VS USER");
                }
                initialise();
                break;
            default:
                boardClicked(v);
                break;
        }
    }
}