/*
 * Author: Quinn M Furgueson
 * Date: 6/6/2024
 * Contact: quinnfurgueson@gmail.com
 * 
 * GPL (GNU Public License)
 * TLDR: free to use, distribute, or modify for anyone,
 * but any derivative published works must also be GPL
 * 
 * I will comment this later to make the code more clear
 * it is very late and I am tired
 * 
 * this will automatically backup any files or directories
 * to a new file or directory in defined intervals. There
 * are preset backup intervals, like every 2 weeks, every
 * week, every day, and every hour. You can also input your
 * own custom amount of time in ms, but it can't be higher
 * than the int limit.
 * 
 * I am using this to backup my minecraft server world
 * 
 * I am trying to find a way to make the javax.swing timer
 * work with long instead of int so we can get backup delays
 * in ms that are longer than the int limit
 * 
 * Future plans: delete backups that are older than certain amount of days
 * {Infinite, 1 Year, 6 Months, 3 Months, 1 Month, 10 days, 5 days, 1 day}
 * 
 */

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import org.apache.commons.io.FileUtils;

public class Backup implements ItemListener {
    //constants
    private static final String[] COMBO_LIST = {"2 Weeks","Week","Day","Hour","Custom"};
    
    private static final int TWO_WEEKS = 1209600000;
    private static final int ONE_WEEK = 604800000;
    private static final int ONE_DAY = 86400000;
    private static final int ONE_HOUR = 3600000;
    
    private static final String SET = "SET";
    private static final String STOP = "STOP";
    private static final String START = "START";
    private static final String SELECT = "SELECT";

    private static final String MS_UNIT = "ms";
    
    private static final String PATH_WARNING_MESSAGE = "One or more paths is not selected!";
    private static final String INPUT_WARNING_MESSAGE = "Invalid Input, only put numbers please! Cannot be larger than " + Integer.MAX_VALUE + " ms";
    private static final String WARNING_TITLE = "Warning";

    private static final String SELECT_BACKUP_FILE_LABEL = "Please Select the FILE TO BACKUP";
    private static final String SELECT_BACKUP_PATH_LABEL = "Please Select the PLACE TO BACKUP";
    private static final String FILE_TO_BACKUP_LABEL = "File to Backup";
    private static final String BACKUP_PATH_LABEL = "Place to Backup File";
    
    private static final String DESKTOP_PATH = "/Desktop";
    private static final String USER_HOME = "user.home";

    private static final String CHECK_FOR_LETTERS = ".*[a-z].*";
    private static final String YEAR_MONTH_DAY_HOUR_MIN_SEC = "yyyy-MM-dd HH.mm.ss";

    private static final int APP_WIDTH = 800;
    private static final int APP_HEIGHT = 600;

    private static final int GRID_ROWS = 3;
    private static final int GRID_COLS = 1;

    private static final String APP_TITLE = "Time Interval Backup";

    //window components
    private static JFrame frame;
    private static JPanel setBackupIncrement, backupFileSelect, startStop;

    private static JComboBox<String> timeInc;
    private static JTextField customTime;
    private static JLabel ms;
    private static JButton setIncButton;

    private static JButton selectOne, selectTwo;
    private static JLabel backupFrom, backupTo;
    private static JFileChooser fileChooser;

    private static JButton start, stop;

    //variables
    private static String backupFromPath, backupToPath;
    private static String selected = COMBO_LIST[0];

    private static int msIncrement = TWO_WEEKS;

    //time components
    private static Timer timer;

    private static DateTimeFormatter dtf = DateTimeFormatter.ofPattern(YEAR_MONTH_DAY_HOUR_MIN_SEC);  
    private static LocalDateTime now = LocalDateTime.now();
    
    public static void main(String[] args) throws IOException {
        frame = new JFrame(APP_TITLE);

        setBackupIncrement = new JPanel();
        backupFileSelect = new JPanel();
        startStop = new JPanel();

        //made purely for action listener
        Backup b = new Backup();

        timeInc = new JComboBox<String>(COMBO_LIST);
        timeInc.addItemListener(b);

        customTime = new JTextField(10);
        customTime.setEnabled(false);
        ms = new JLabel(MS_UNIT);
        
        //disable custom button by default
        setIncButton = new JButton(SET);
        setIncButton.setEnabled(false);

        //if pressed, warning if custom time is blank, contains letters
        //or larger than int limit, otherwise, set custom time interval
        setIncButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String incNum = customTime.getText().toLowerCase();

                if(incNum.isBlank() || incNum.matches(CHECK_FOR_LETTERS) || Long.parseLong(incNum) > Integer.MAX_VALUE) {
                    JOptionPane.showMessageDialog(frame, INPUT_WARNING_MESSAGE, WARNING_TITLE, JOptionPane.WARNING_MESSAGE);
                }
                else {
                    msIncrement = Integer.parseInt(incNum);
                    System.out.println(msIncrement);
                }
            }
        });

        backupFrom = new JLabel(FILE_TO_BACKUP_LABEL);
        backupTo = new JLabel(BACKUP_PATH_LABEL);

        //set default file choose path to desktop
        fileChooser = new JFileChooser(System.getProperty(USER_HOME) + DESKTOP_PATH);
        fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);

        selectOne = new JButton(SELECT);
        //choose dir/file and store chosen path of backup dir/file
        selectOne.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                fileChooser.showDialog(null, SELECT_BACKUP_FILE_LABEL);
                fileChooser.setVisible(true);

                if(fileChooser.getSelectedFile() != null)
                {
                    backupFromPath = fileChooser.getSelectedFile().getAbsolutePath();
                    selectOne.setBackground(Color.GREEN);
                }
            }
        });

        selectTwo = new JButton(SELECT);
        //choose backup location and store backup path
        selectTwo.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                fileChooser.showDialog(null, SELECT_BACKUP_PATH_LABEL);
                fileChooser.setVisible(true);
                
                if(fileChooser.getSelectedFile() != null)
                {
                    backupToPath = fileChooser.getSelectedFile().getAbsolutePath();
                    selectTwo.setBackground(Color.GREEN);
                }
            }
        });

        start = new JButton(START);
        //start timer to check if time interval has passed, then copy dir/file
        //to backup location named the date and time of backup
        start.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                timer = new Timer(msIncrement, new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        try {
                            now = LocalDateTime.now();
        
                            //get extension of file to properly copy it over
                            String extension = "";
                            int i = backupFromPath.lastIndexOf(".");
                            int p = Math.max(backupFromPath.lastIndexOf('/'), backupFromPath.lastIndexOf('\\'));
        
                            if (i > p) {
                                extension = "." + backupFromPath.substring(i+1);
                            }
        
                            //prep backup dir/file and location
                            File fileToBackup = new File(backupFromPath);
                            File backupFile = new File(backupToPath + "/" + dtf.format(now) + extension);
        
                            //if the file is a directory, copy it and its contents to the backup location
                            //else copy just the file to the backup location
                            if(fileToBackup.isDirectory())
                                FileUtils.copyDirectory(fileToBackup, backupFile);
                            else
                                Files.copy(fileToBackup.toPath(), backupFile.toPath());
        
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }
                    }
                });
                //save one backup immediately
                timer.setInitialDelay(0);

                //if any of the backups are blank warn the user and don't start timer
                //else disable the start button (user feedback) and start timer
                if(backupFromPath == null || backupFromPath.isEmpty() || backupToPath == null || backupToPath.isEmpty()) {
                    JOptionPane.showMessageDialog(frame, PATH_WARNING_MESSAGE, WARNING_TITLE, JOptionPane.WARNING_MESSAGE);
                }
                else
                {
                    start.setBackground(Color.WHITE);
                    start.setEnabled(false);
    
                    timer.restart();
                }
            }
        });

        stop = new JButton(STOP);
        //enable and turn start button green and stop timer
        stop.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                start.setBackground(Color.GREEN);
                start.setEnabled(true);

                timer.stop(); 
            }
        });

        start.setBackground(Color.GREEN);
        stop.setBackground(Color.RED);

        //formatting the window
        backupFileSelect.add(backupFrom);
        backupFileSelect.add(selectOne);
        backupFileSelect.add(backupTo);
        backupFileSelect.add(selectTwo);

        setBackupIncrement.add(timeInc);
        setBackupIncrement.add(customTime);
        setBackupIncrement.add(ms);
        setBackupIncrement.add(setIncButton);

        startStop.add(start);
        startStop.add(stop);

        frame.add(setBackupIncrement);
        frame.add(backupFileSelect);
        frame.add(startStop);

        setBackupIncrement.setLayout(new FlowLayout());
        backupFileSelect.setLayout(new FlowLayout());
        startStop.setLayout(new FlowLayout());

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new GridLayout(GRID_ROWS, GRID_COLS));
        frame.setSize(new Dimension(APP_WIDTH, APP_HEIGHT));
        frame.setVisible(true);
    }

    //check what ComboBox option is selected and set relevant time delay
    @Override
    public void itemStateChanged(ItemEvent e) {
        if (e.getSource() == timeInc) {
            selected = (String) timeInc.getSelectedItem();
            
            switch(selected) {
                // one month in ms breaks the int limit, can't use long because timer only uses int delay
                case "2 Weeks":
                    customTime.setEnabled(false);
                    setIncButton.setEnabled(false);
                    msIncrement = TWO_WEEKS;
                    break;
                case "Week":
                    customTime.setEnabled(false);
                    setIncButton.setEnabled(false);
                    msIncrement = ONE_WEEK;
                    break;
                case "Day":
                    customTime.setEnabled(false);
                    setIncButton.setEnabled(false);
                    msIncrement = ONE_DAY;
                    break;
                case "Hour":
                    customTime.setEnabled(false);
                    setIncButton.setEnabled(false);
                    msIncrement = ONE_HOUR;
                    break;
                default:
                    customTime.setEnabled(true);
                    setIncButton.setEnabled(true);
                    break;
            }
        }
    }
}