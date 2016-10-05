import java.util.*;
public class BeamTsk extends TimerTask {
    
    private GUI canvas;
    /** Creates a new instance of BeamTsk */
    public BeamTsk(GUI canvas) {
        this.canvas=canvas;
    }
    
    public void run() {
        //if (canvas.count>5) 
       //     canvas.cancel=true;
        if (canvas.i<3)
            canvas.i=canvas.i+1;
        else
            canvas.i=0;
        
        switch (canvas.i){
        case 1:{
            canvas.x=30;
            canvas.y=30;
            canvas.ht=10;
            break;
       }
            case 2 :{
           canvas.x=canvas.x+5;
           canvas.y=canvas.y-3;
           canvas.ht=canvas.ht+6;
           break;
        }
       case 3:{
           canvas.x=canvas.x+5;
           canvas.y=canvas.y-3;
           canvas.ht=canvas.ht+6; 
           canvas.count=canvas.count+1;
           break;
       } 
    }
    canvas.repaint();
    }
}
