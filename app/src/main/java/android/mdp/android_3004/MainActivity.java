package android.mdp.android_3004;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import java.util.Random;

public class MainActivity extends AppCompatActivity {

    final int MAZE_CELL_C = 15;
    final int MAZE_CELL_R = 20;
    GridLayout grid_maze;
    ImageView robot;
    int robot_location = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        int[] colorlist = {Color.RED, Color.MAGENTA, Color.GREEN, Color.CYAN, Color.BLUE};

        grid_maze = (GridLayout) findViewById(R.id.grid_maze);
        grid_maze.setColumnCount(MAZE_CELL_C);
        grid_maze.setRowCount(MAZE_CELL_R);

        Drawable d_box = this.getResources().getDrawable(R.drawable.d_box, null);
        d_box.setColorFilter(colorlist[new Random().nextInt(5)], PorterDuff.Mode.ADD);

        for (int i = 0; i < MAZE_CELL_C*MAZE_CELL_R; i++) {
            TextView tv = new TextView(this);
            tv.setGravity(Gravity.CENTER);
            tv.setBackground(d_box);
            tv.setText(String.valueOf(i));

            grid_maze.addView(tv);
        }

        createRobot();
    }

    protected void createRobot() {
        int c = MAZE_CELL_C-2, r = MAZE_CELL_R-2;

        Drawable d_robot = this.getResources().getDrawable(R.drawable.d_robot2, null);
        robot = new ImageView(this);
        robot.setImageDrawable(d_robot);
        GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
        lp.columnSpec = GridLayout.spec(c, 2);
        lp.rowSpec = GridLayout.spec(r, 2);
        lp.height = 80;
        lp.width = 80;
        robot.setLayoutParams(lp);
        grid_maze.addView(robot);
        robot_location = (r * MAZE_CELL_C) + c;
    }

    protected void robot_rotate(int direction) {
        robot.setRotation(robot.getRotation() + (direction*90));
    }

    protected void robote_move() {
//        0: UP | 1: RIGHT | 2: DOWN | 3: LEFT

        int col = robot_location%MAZE_CELL_C,
                row = robot_location/MAZE_CELL_C,
                direction = (((int)robot.getRotation())%360) / 90;

        if ((direction == 0 && (robot_location/MAZE_CELL_C) > 0) ||
                (direction == 1 && (robot_location%MAZE_CELL_C) < (MAZE_CELL_C-2)) ||
                (direction == 2 && (robot_location/MAZE_CELL_C) < (MAZE_CELL_R-2)) ||
                (direction == 3 && (robot_location%MAZE_CELL_C) > 0)) {

            int addon = 1;
            if (direction == 0 || direction == 2) addon = MAZE_CELL_C;
            if (direction == 0 || direction == 3) addon *= -1;
            robot_location += addon;

            GridLayout.LayoutParams lp = (GridLayout.LayoutParams) robot.getLayoutParams();
            if (direction == 0 || direction == 2) {
                lp.rowSpec = GridLayout.spec(row + ((direction == 0) ? -1 : 1), 2);
            } else {
                lp.columnSpec = GridLayout.spec(col + ((direction == 3) ? -1 : 1), 2);
            }
            robot.setLayoutParams(lp);
        }
    }

    protected void direction_click(View v) {
        int id = v.getId();
        boolean move = false;
        int direction = -1;
        switch (id)
        {
            case R.id.btn_up: robote_move();
                break;
            case R.id.btn_down: robot_rotate(2);
                break;
            case R.id.btn_left: robot_rotate(3);
                break;
            case R.id.btn_right: robot_rotate(1);
                break;
        }
    }

    protected void toggle_timer(View v) {
        Switch s = (Switch)v;
        if(("Explore").equalsIgnoreCase(s.getText().toString())) {
            s.setText("Fastest");
        } else {
            s.setText("Explore");
        }
    }

    protected void onclick_timer(View v) {
        Button b = (Button)v;
        if(("Start Timer").equalsIgnoreCase(b.getText().toString())) {
            b.setText("Stop Timer");
        } else {
            b.setText("Start Timer");
        }
    }

    protected void onclick_reset(View v) {
        Switch s = (Switch) findViewById(R.id.swt_isfast);
        TextView tv = (TextView)(s.isChecked()?findViewById(R.id.txt_timefast):findViewById(R.id.txt_timeexplore));
        tv.setText("00.00.00");
    }
}
