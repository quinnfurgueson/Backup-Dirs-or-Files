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
    private static String[] comboList = {"2 Weeks","Week","Day","Hour","Custom"};

    private static JFrame frame;
    private static JPanel setBackupIncrement, backupFileSelect, startStop;

    private static JComboBox<String> timeInc;
    private static JTextField customTime;
    private static JLabel ms;
    private static JButton setIncButton;

    private static JButton selectOne, selectTwo;
    private static JLabel backupFrom, backupTo;
    private static JFileChooser fileChooser;

    private static String backupFromPath, backupToPath;
    String selected = "2 Weeks";

    private static JButton start, stop;

    private static int msIncrement = 1209600000;

    private static Timer timer;

    private static DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH.mm.ss");  
    private static LocalDateTime now = LocalDateTime.now();
    
    public static void main(String[] args) throws IOException {
        frame = new JFrame("TimedBackupFiles");

        setBackupIncrement = new JPanel();
        backupFileSelect = new JPanel();
        startStop = new JPanel();

        Backup b = new Backup();

        timeInc = new JComboBox<String>(comboList);
        timeInc.addItemListener(b);

        customTime = new JTextField(10);
        customTime.setEnabled(false);
        ms = new JLabel("ms");
        
        setIncButton = new JButton("SET");
        setIncButton.setEnabled(false);

        setIncButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String incNum = customTime.getText().toLowerCase();

                if(incNum.isBlank() || incNum.matches(".*[a-z].*") || Long.parseLong(incNum) > Integer.MAX_VALUE) {
                    JOptionPane.showMessageDialog(frame, "Invalid Input, only put numbers please! Cannot be larger than " + Integer.MAX_VALUE + " ms",
                                "Warning", JOptionPane.WARNING_MESSAGE);
                }
                else {
                    msIncrement = Integer.parseInt(incNum);
                    System.out.println(msIncrement);
                }
            }
        });

        backupFrom = new JLabel("File to Backup");
        backupTo = new JLabel("Place to Backup File");

        fileChooser = new JFileChooser(System.getProperty("user.home") + "/Desktop");
        fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);

        selectOne = new JButton("SELECT");
        selectOne.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                fileChooser.showDialog(null,"Please Select the FILE TO BACKUP");
                fileChooser.setVisible(true);

                if(fileChooser.getSelectedFile() != null)
                {
                    backupFromPath = fileChooser.getSelectedFile().getAbsolutePath();
                    selectOne.setBackground(new Color(0, 250, 0));
                }
            }
        });

        selectTwo = new JButton("SELECT");
        selectTwo.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                fileChooser.showDialog(null,"Please Select the PLACE TO BACKUP");
                fileChooser.setVisible(true);
                
                if(fileChooser.getSelectedFile() != null)
                {
                    backupToPath = fileChooser.getSelectedFile().getAbsolutePath();
                    selectTwo.setBackground(new Color(0, 250, 0));
                }
            }
        });

        start = new JButton("Start");
        start.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                timer = new Timer(msIncrement, new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        try {
                            now = LocalDateTime.now();
        
                            String extension = "";
                            int i = backupFromPath.lastIndexOf(".");
                            int p = Math.max(backupFromPath.lastIndexOf('/'), backupFromPath.lastIndexOf('\\'));
        
                            if (i > p) {
                                extension = "." + backupFromPath.substring(i+1);
                            }
        
                            File fileToBackup = new File(backupFromPath);
                            File backupFile = new File(backupToPath + "/" + dtf.format(now) + extension);
        
                            if(fileToBackup.isDirectory())
                                FileUtils.copyDirectory(fileToBackup, backupFile);
                            else
                                Files.copy(fileToBackup.toPath(), backupFile.toPath());
        
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }
                    }
                });
                timer.setInitialDelay(0);

                if(backupFromPath == null || backupFromPath.isEmpty() || backupToPath == null || backupToPath.isEmpty()) {
                    JOptionPane.showMessageDialog(frame, "One or more paths is not selected!",
                                "Warning", JOptionPane.WARNING_MESSAGE);
                }
                else
                {
                    start.setBackground(new Color(250,250,250));
                    start.setEnabled(false);
    
                    timer.restart();
                }
            }
        });

        stop = new JButton("Stop");
        stop.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                start.setBackground(new Color(0,250,0));
                start.setEnabled(true);

                timer.stop(); 
            }
        });

        start.setBackground(new Color(0,250,0));
        stop.setBackground(new Color(250,0,0));

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
        frame.setLayout(new GridLayout(3, 1));
        frame.setSize(new Dimension(800, 600));
        frame.setVisible(true);
    }

    public void setInc()
    {
        String incNum = customTime.getText().toLowerCase();

        if(incNum.isBlank() || incNum.matches(".*[a-z].*")) {
            JOptionPane.showMessageDialog(frame, "Invalid Input, only put numbers please!",
                        "Warning", JOptionPane.WARNING_MESSAGE);
        }
        else {
            msIncrement = Integer.parseInt(customTime.getText());
        }
    }

    @Override
    public void itemStateChanged(ItemEvent e) {
        if (e.getSource() == timeInc) {
            selected = (String) timeInc.getSelectedItem();
            System.out.println(selected);
            switch(selected) {
                // one month in ms breaks the int limit, can't use long because timer only uses ints
                case "2 Weeks":
                    customTime.setEnabled(false);
                    setIncButton.setEnabled(false);
                    msIncrement = 1209600000; //1 week in ms
                    break;
                case "Week":
                    customTime.setEnabled(false);
                    setIncButton.setEnabled(false);
                    msIncrement = 604800000; //1 week in ms
                    break;
                case "Day":
                    customTime.setEnabled(false);
                    setIncButton.setEnabled(false);
                    msIncrement = 86400000;
                    break;
                case "Hour":
                    customTime.setEnabled(false);
                    setIncButton.setEnabled(false);
                    msIncrement = 3600000;
                    break;
                default:
                    customTime.setEnabled(true);
                    setIncButton.setEnabled(true);
                    break;
            }
        }
    }
}
