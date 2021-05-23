package org.cloudbus.cloudsim.sdn.graph.dialog;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.concurrent.ExecutionException;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;
import javax.swing.Timer;

import org.cloudbus.cloudsim.sdn.graph.example.GraphicSDNExample;

public class SDNRun extends JDialog {
	private static final long serialVersionUID = -8313194085507492462L;
	
	private String physicalTopologyFile = "";  //physical
	private String deploymentFile = "";        //virtual
	private String workloads_background = "";  //workload
	private String workloads = "";             //workload
	
	private JPanel panel;
	private JScrollPane pane;
	private JTextArea outputArea;
	private JLabel imageLabel;
	private JLabel msgLabel;
	private JComponent space;
	private int counter = 0;
	private Timer timer;
	
	private GraphicSDNExample sdn;

	public SDNRun(final String phy, final String vir, final String wlbk, final String wl, final JFrame frame){
		physicalTopologyFile = phy;
		deploymentFile = vir;
		workloads_background = wlbk;
		workloads = wl;
		
		setLayout(new BorderLayout());
		
		panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        initUI();
        run();
        add(panel, BorderLayout.CENTER);
		
		setDefaultCloseOperation(DISPOSE_ON_CLOSE );
		setTitle("Run Simulation");
		setModal(true);
		setPreferredSize(new Dimension(900, 600));
		setResizable(false);
		pack();
		setLocationRelativeTo(frame); // must be called between pack and setVisible to work properly
		setVisible(true);
	}
	
	private void initUI(){
		ImageIcon ii = new ImageIcon(this.getClass().getResource("/src/1.gif"));
		imageLabel = new JLabel(ii);
		imageLabel.setAlignmentX(CENTER_ALIGNMENT);
		msgLabel = new JLabel("Simulation is executing");
		msgLabel.setAlignmentX(CENTER_ALIGNMENT);
		space = (JComponent)Box.createRigidArea(new Dimension(0, 200));
		panel.add(space);
		panel.add(msgLabel);
        panel.add(imageLabel);
        
        pane = new JScrollPane();
        outputArea = new JTextArea();

        outputArea.setLineWrap(true);
        outputArea.setWrapStyleWord(true);
        outputArea.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        outputArea.setEditable(false);
        //outputArea.setText("wo ai bei jin tian an men");
        //readFile(physicalTopologyFile, outputArea);

        pane.getViewport().add(outputArea);
        panel.add(pane);
        pane.setVisible(false);
	}
	
	private void run(){
		
		sdn = new GraphicSDNExample(physicalTopologyFile, deploymentFile, workloads_background, workloads, outputArea);   	
    	
        SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
    		protected Boolean doInBackground() throws Exception {
    			boolean success = false;
    			success = sdn.simulate();
    	    	if(success){
    	    		sdn.output();
    	    		append("<<<<<<<<<< Simulation completed >>>>>>>>>");
    	    	}else{
    	    		append("<<<<<<<<<< Running Error >>>>>>>>>>");
    	    	}
				return success;
    		}

    		protected void done() {
    		    boolean status;
    		    try {
    		    	status = get();
    				panel.remove(space);
    		    	panel.remove(imageLabel);
    		    	panel.remove(msgLabel);
    		    	pane.setVisible(true);
    		    	panel.revalidate(); 
    		    	panel.repaint();
    		     
    		    } catch (InterruptedException e) {

    		    } catch (ExecutionException e) {

    		    }
    		 }
        };
    	worker.execute();
	}
	
	private void append(String content){
		outputArea.append(content+"\n");
	}
	
	/** below only used for testing reading file to textarea */
	private void startTest(){
        ActionListener updateProBar = new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                if(counter>=100){
                	timer.stop();
                	panel.remove(space);
                	panel.remove(imageLabel);
                	panel.remove(msgLabel);
                	pane.setVisible(true);
                	panel.revalidate(); 
                	panel.repaint();
           
                }else{
                	counter +=2;
                }
            }
        };
        timer = new Timer(50, updateProBar);
        timer.start();
	}
    private void readFile(String path, JTextArea area) {

    	try{
            FileReader reader = new FileReader(path);
            BufferedReader br = new BufferedReader(reader);
            area.read( br, null );
            br.close();
            area.requestFocus();
        }catch(Exception e2) {
        	System.out.println(e2); 
        }
    }
    
}
