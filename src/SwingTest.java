/* TopLevelDemo.java requires no other files. */

import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Hashtable;
import javax.swing.*;
import javax.swing.text.*;
import javax.swing.text.html.*;


public class SwingTest {

    private static JEditorPane mainPane;

    private static void createAndShowGUI() {
        //Create and set up the window.
        JFrame frame = new JFrame("F5");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        //Create the menu bar.  Make it have a green background.
        JMenuBar greenMenuBar = new JMenuBar();
        greenMenuBar.setOpaque(true);
        greenMenuBar.setBackground(new Color(154, 165, 127));
        greenMenuBar.setPreferredSize(new Dimension(200, 20));

        //Create a yellow label to put in the content pane.
        mainPane = new JTextPane();

        //EditorKit htmlKit = new HTMLEditorKit();
        //HTMLDocument doc = new HTMLDocument();

        //mainPane.setEditorKit(htmlKit);
        //mainPane.setDocument(doc);

        mainPane.setContentType("text/html");
        mainPane.setText("<div id='mainDiv'></div>");


        mainPane.setEditable(false);
        mainPane.setBackground(new Color(255, 102, 153));
        mainPane.setPreferredSize(new Dimension(400, 360));

        JScrollPane scrollPane = new JScrollPane(mainPane);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        frame.setContentPane(scrollPane);

        //Set the menu bar and add the label to the content pane.
        frame.setJMenuBar(greenMenuBar);
        //frame.getContentPane().add(scrollPane, BorderLayout.CENTER);

        //Display the window.
        frame.pack();
        frame.setVisible(true);
    }

    private static void startUpdateThread() {
        Thread updateThread = new Thread() {
            public void run() {
                try {
                    downloadPosts();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        updateThread.start();
    }

    private static void downloadPosts() throws Exception {
        Downloader downloader = new Downloader("", "", 3488618);

        while(true) {
            ArrayList<Hashtable<String, String>> newPosts = downloader.getNewPosts();
            if (newPosts != null)
                updateGUI(newPosts);
            Thread.currentThread().sleep(3000);
        }
    }

    private static void updateGUI(final ArrayList<Hashtable<String, String>> posts) {
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                HTMLDocument doc = (HTMLDocument)mainPane.getDocument();
                Element div = doc.getElement("mainDiv");

                for (Hashtable<String, String> post : posts) {
                    try {
                        doc.insertBeforeEnd(div, "<strong>" + post.get("username") + " </strong>");
                        doc.insertBeforeEnd(div, post.get("timestamp") + "<br>");
                        doc.insertBeforeEnd(div, post.get("post") + "<br><br>");

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

            }
        });
    }

    public static void main(String[] args) {
        //Schedule a job for the event-dispatching thread:
        //creating and showing this application's GUI.
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGUI();
            }
        });

        startUpdateThread();
    }
}

