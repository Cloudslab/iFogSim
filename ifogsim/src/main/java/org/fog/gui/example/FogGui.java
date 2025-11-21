package org.fog.gui.example;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.fog.gui.core.Bridge;
import org.fog.gui.core.Graph;
import org.fog.gui.core.GraphView;
import org.fog.gui.dialog.AddActuator;
import org.fog.gui.dialog.AddFogDevice;
import org.fog.gui.dialog.AddLink;
import org.fog.gui.dialog.AddPhysicalEdge;
import org.fog.gui.dialog.AddPhysicalNode;
import org.fog.gui.dialog.AddSensor;
import org.fog.gui.dialog.SDNRun;


public class FogGui extends JFrame {
	private static final long serialVersionUID = -2238414769964738933L;
	
	private JPanel contentPane;
	
	/** Import file names */
	private String physicalTopologyFile = "";  //physical
	private String deploymentFile = "";        //virtual
	private String workloads_background = "";  //workload
	private String workloads = "";             //workload

	private JPanel panel;
	private JPanel graph;
	
	private Graph physicalGraph;
	//private Graph virtualGraph;
	private GraphView physicalCanvas;
	//private GraphView virtualCanvas;
	
	private JButton btnRun;
	
	private String mode;  //'m':manual; 'i':import

	public FogGui() {
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setPreferredSize(new Dimension(1280, 800));
        setLocationRelativeTo(null);
        //setResizable(false);
        
        setTitle("Fog Topology Creator");
        contentPane = new JPanel();
		setContentPane(contentPane);
		contentPane.setLayout(new BorderLayout());
		
		initUI();
		initGraph();
		
		pack();
		setVisible(true);
	}
	
	public final void initUI() {
		setUIFont (new javax.swing.plaf.FontUIResource("Serif",Font.BOLD,18));

        panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        
        graph = new JPanel(new java.awt.GridLayout(1, 2));
        
		initBar();
		doPosition();
	}
	
	/** position window */
	private void doPosition() {

		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int height = screenSize.height;
		int width = screenSize.width;

		int x = (width / 2 - 1280 / 2);
		int y = (height / 2 - 800 / 2);
		// One could use the dimension of the frame. But when doing so, one have to call this method !BETWEEN! pack and
		// setVisible. Otherwise the calculation will go wrong.

		this.setLocation(x, y);
	}
	
	/** Initialize project menu and tool bar */
    private final void initBar() {
    	//---------- Start ActionListener ----------
    	ActionListener readPhyTopoListener = new ActionListener() {
		    public void actionPerformed(ActionEvent e) {
		    	physicalTopologyFile = importFile("josn");
            	checkImportStatus();
		    }
		};
		ActionListener readVirTopoListener = new ActionListener() {
		    public void actionPerformed(ActionEvent e) {
		    	deploymentFile = importFile("json");
            	checkImportStatus();
		    }
		};
		ActionListener readWorkloadBkListener = new ActionListener() {
		    public void actionPerformed(ActionEvent e) {
		    	workloads_background = importFile("cvs");
		    	checkImportStatus();
		    }
		};
		ActionListener readWorkloadListener = new ActionListener() {
		    public void actionPerformed(ActionEvent e) {
		    	workloads = importFile("cvs");
		    	checkImportStatus();
		    }
		};
		
		ActionListener addFogDeviceListener = new ActionListener() {
		    public void actionPerformed(ActionEvent e) {
		    	openAddFogDeviceDialog();
		    }
		};
		
		ActionListener addPhysicalNodeListener = new ActionListener() {
		    public void actionPerformed(ActionEvent e) {
		    	openAddPhysicalNodeDialog();
		    }
		};

		ActionListener addPhysicalEdgeListener = new ActionListener() {
		    public void actionPerformed(ActionEvent e) {
		    	openAddPhysicalEdgeDialog();
		    }
		};
		
		ActionListener addLinkListener = new ActionListener() {
		    public void actionPerformed(ActionEvent e) {
		    	openAddLinkDialog();
		    }
		};
		
		ActionListener addActuatorListener = new ActionListener() {
		    public void actionPerformed(ActionEvent e) {
		    	openAddActuatorDialog();
		    }
		};
		
		ActionListener addSensorListener = new ActionListener() {
		    public void actionPerformed(ActionEvent e) {
		    	openAddSensorDialog();
		    }
		};
		ActionListener importPhyTopoListener = new ActionListener() {
		    public void actionPerformed(ActionEvent e) {
		    	String fileName = importFile("josn");
		    	Graph phyGraph= Bridge.jsonToGraph(fileName, 0);
		    	physicalGraph = phyGraph;
		    	physicalCanvas.setGraph(physicalGraph);
		    	physicalCanvas.repaint();
		    }
		};
		
		ActionListener savePhyTopoListener = new ActionListener() {
		    public void actionPerformed(ActionEvent e) {
		    	try {
					saveFile("json", physicalGraph);
				} catch (IOException e1) {
					e1.printStackTrace();
				}
		    }
		};
				
		//---------- End ActionListener ----------
    	
        //---------- Start Creating project tool bar ----------
        JToolBar toolbar = new JToolBar();
                
        ImageIcon iSensor = new ImageIcon(
                getClass().getResource("/images/sensor.png"));
        ImageIcon iActuator = new ImageIcon(
                getClass().getResource("/images/actuator.png"));
        ImageIcon iFogDevice = new ImageIcon(
                getClass().getResource("/images/dc.png"));
        ImageIcon iLink = new ImageIcon(
                getClass().getResource("/images/hline2.png"));
        ImageIcon iHOpen = new ImageIcon(
                getClass().getResource("/images/openPhyTop.png"));
        ImageIcon iHSave = new ImageIcon(
                getClass().getResource("/images/savePhyTop.png"));
        
        ImageIcon run = new ImageIcon(
                getClass().getResource("/images/play.png"));
        ImageIcon exit = new ImageIcon(
                getClass().getResource("/images/exit.png"));

        final JButton btnSensor = new JButton(iSensor);
        btnSensor.setToolTipText("Add Sensor");
        final JButton btnActuator = new JButton(iActuator);
        btnActuator.setToolTipText("Add Actuator");
        
        final JButton btnFogDevice = new JButton(iFogDevice);
        btnFogDevice.setToolTipText("Add Fog Device");
        final JButton btnLink = new JButton(iLink);
        btnLink.setToolTipText("Add Link");
        
        
        final JButton btnHopen = new JButton(iHOpen);
        btnHopen.setToolTipText("Open Physical Topology");
        final JButton btnHsave = new JButton(iHSave);
        btnHsave.setToolTipText("Save Physical Topology");
        
        btnRun = new JButton(run);
        btnRun.setToolTipText("Start simulation");
        JButton btnExit = new JButton(exit);
        btnExit.setToolTipText("Exit CloudSim");
        toolbar.setAlignmentX(0);
        
        
        btnSensor.addActionListener(addSensorListener);
        btnActuator.addActionListener(addActuatorListener);
        btnFogDevice.addActionListener(addFogDeviceListener);
        btnLink.addActionListener(addLinkListener);
        btnHopen.addActionListener(importPhyTopoListener);
        btnHsave.addActionListener(savePhyTopoListener);
        
        btnRun.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
            	if("i"==mode){
            		if(physicalTopologyFile==null || physicalTopologyFile.isEmpty()){
            			JOptionPane.showMessageDialog(panel, "Please select physicalTopologyFile", "Error", JOptionPane.ERROR_MESSAGE);
            			return;
            		}
            		if(deploymentFile==null || deploymentFile.isEmpty()){
            			JOptionPane.showMessageDialog(panel, "Please select deploymentFile", "Error", JOptionPane.ERROR_MESSAGE);
            			return;
            		}
            		if(workloads_background==null || workloads_background.isEmpty()){
            			JOptionPane.showMessageDialog(panel, "Please select workloads_background", "Error", JOptionPane.ERROR_MESSAGE);
            			return;
            		}
            		if(workloads==null || workloads.isEmpty()){
            			JOptionPane.showMessageDialog(panel, "Please select workloads", "Error", JOptionPane.ERROR_MESSAGE);
            			return;
            		}
            		// run simulation
            		SDNRun run = new SDNRun(physicalTopologyFile, deploymentFile, 
            								workloads_background, workloads, FogGui.this);

            		
		        }else if("m"==mode){
		        	
		        }
            	
            }
        });
        btnExit.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                System.exit(0);
            }

        });       

        toolbar.add(btnSensor);
        toolbar.add(btnActuator);
        toolbar.add(btnFogDevice);
        toolbar.add(btnLink);
        toolbar.add(btnHopen);
        toolbar.add(btnHsave);
        
        toolbar.addSeparator();
        
        /*toolbar.add(btnSensorModule);
        toolbar.add(btnActuatorModule);
        toolbar.add(btnModule);
        toolbar.add(btnAppEdge);*/
        
        toolbar.addSeparator();
        
        toolbar.add(btnRun);
        toolbar.add(btnExit);

        panel.add(toolbar);
        
        contentPane.add(panel, BorderLayout.NORTH);
        //---------- End Creating project tool bar ----------
        
        
        
    	//---------- Start Creating project menu bar ----------
    	//1-1
        JMenuBar menubar = new JMenuBar();
        //ImageIcon iconNew = new ImageIcon(getClass().getResource("/src/new.png"));

        //2-1
        JMenu graph = new JMenu("Graph");
        graph.setMnemonic(KeyEvent.VK_G);
        
        //Graph by importing json and cvs files
        final JMenuItem MiPhy = new JMenuItem("Physical Topology");
        final JMenuItem MiVir = new JMenuItem("Virtual Topology");
        final JMenuItem MiWl1 = new JMenuItem("Workload Background");
        final JMenuItem MiWl2 = new JMenuItem("Workload");
        //Graph drawing elements
        final JMenu MuPhy = new JMenu("Physical");
        JMenuItem MiFogDevice = new JMenuItem("Add Fog Device");
        JMenuItem MiPhyEdge = new JMenuItem("Add Edge");
        JMenuItem MiPhyOpen = new JMenuItem("Import Physical Topology");
        JMenuItem MiPhySave = new JMenuItem("Save Physical Topology");
        MuPhy.add(MiFogDevice);
        MuPhy.add(MiPhyEdge);
        MuPhy.add(MiPhyOpen);
        MuPhy.add(MiPhySave);
        
        MiPhy.addActionListener(readPhyTopoListener);
        MiVir.addActionListener(readVirTopoListener);
        MiWl1.addActionListener(readWorkloadBkListener);
        MiWl2.addActionListener(readWorkloadListener);
             
        MiFogDevice.addActionListener(addFogDeviceListener);
        MiPhyEdge.addActionListener(addPhysicalEdgeListener);
        MiPhyOpen.addActionListener(importPhyTopoListener);
        MiPhySave.addActionListener(savePhyTopoListener);

        graph.add(MuPhy);
        //graph.add(MuVir);
        graph.add(MiPhy);
        //graph.add(MiVir);
        graph.add(MiWl1);
        graph.add(MiWl2);

        //2-2
        JMenu view = new JMenu("View");
        view.setMnemonic(KeyEvent.VK_F);
        
        //switch mode between manual mode (to create graph by hand) and import mode (to create graph from file)
		ActionListener actionSwitcher = new ActionListener() {
		    public void actionPerformed(ActionEvent e) {
		        try {
		    	    String cmd = e.getActionCommand();
		    	    if("Canvas" == cmd){
		    	    	btnSensor.setVisible(true);
		    	    	btnActuator.setVisible(true);
		    	    	btnFogDevice.setVisible(true);
		    	    	btnLink.setVisible(true);
		    	    	btnHopen.setVisible(true);
		    	    	btnHsave.setVisible(true);
		    	    	
		    	    	MiPhy.setVisible(false);
		    	    	MiVir.setVisible(false);
		    	    	MiWl1.setVisible(false);
		    	    	MiWl2.setVisible(false);
		    	    	MuPhy.setVisible(true);
		    	    	//MuVir.setVisible(true);
		    	    	
		    	    	btnRun.setVisible(false);
		    	    	btnRun.setEnabled(false);
		    	    	
		    	    	mode = "m";
		    	    	
		    	    }else if("Execution" == cmd){
		    	    	btnSensor.setVisible(false);
		    	    	btnActuator.setVisible(false);
		    	    	btnFogDevice.setVisible(false);
		    	    	btnLink.setVisible(false);
		    	    	btnHopen.setVisible(false);
		    	    	btnHsave.setVisible(false);
		    	    	
		    	    	MiPhy.setVisible(true);
		    	    	MiVir.setVisible(true);
		    	    	MiWl1.setVisible(true);
		    	    	MiWl2.setVisible(true);
		    	    	MuPhy.setVisible(false);
		    	    	//MuVir.setVisible(false);
		    	    	
		    	    	btnRun.setVisible(true);
		    	    	btnRun.setEnabled(false);
		    	    	
		    	    	mode = "i";
		    	    }
		    	    //System.out.println(e.getActionCommand());
		        } catch (Exception ex) {
		            ex.printStackTrace();
		        }
		    }
		};
        JRadioButtonMenuItem manualMode = new JRadioButtonMenuItem("Canvas");
        manualMode.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_U, ActionEvent.CTRL_MASK));
        manualMode.addActionListener(actionSwitcher);
        JRadioButtonMenuItem importMode = new JRadioButtonMenuItem("Execution");
        importMode.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_I, ActionEvent.CTRL_MASK));
        importMode.addActionListener(actionSwitcher);
        ButtonGroup group = new ButtonGroup();
        group.add(manualMode);
        group.add(importMode);
        
        JMenuItem fileExit = new JMenuItem("Exit");
        fileExit.setMnemonic(KeyEvent.VK_C);
        fileExit.setToolTipText("Exit CloudSim");
        fileExit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W,
            ActionEvent.CTRL_MASK));

        fileExit.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                System.exit(0);
            }

        });

        view.add(manualMode);
        view.add(importMode);
        view.addSeparator();
        view.add(fileExit);        

        
        //3-1
        menubar.add(view);
        menubar.add(graph);

        //4-1
        setJMenuBar(menubar);
        //----- End Creating project menu bar -----
        
        
        
        //----- Start Initialize menu and tool bar -----
        manualMode.setSelected(true);
        mode = "m";
        
        //btnHost.setVisible(true);
        btnSensor.setVisible(true);
        btnActuator.setVisible(true);
        btnFogDevice.setVisible(true);
        btnLink.setVisible(true);
    	btnHopen.setVisible(true);
    	btnHsave.setVisible(true);
    	
    	MiPhy.setVisible(false);
    	MiVir.setVisible(false);
    	MiWl1.setVisible(false);
    	MiWl2.setVisible(false);
    	MuPhy.setVisible(true);
    	//MuVir.setVisible(true);
    	
    	btnRun.setVisible(false);
    	btnRun.setEnabled(false);
        //----- End Initialize menu and tool bar -----

    }

	protected void openAddActuatorDialog() {
		AddActuator actuator = new AddActuator(physicalGraph, FogGui.this);
		physicalCanvas.repaint();
	}
	
	protected void openAddLinkDialog() {
		AddLink phyEdge = new AddLink(physicalGraph, FogGui.this);
    	physicalCanvas.repaint();
		
	}

	protected void openAddFogDeviceDialog() {
		AddFogDevice fogDevice = new AddFogDevice(physicalGraph, FogGui.this);
    	physicalCanvas.repaint();
		
	}

	/** initialize Canvas */
    private void initGraph(){
    	physicalGraph = new Graph();
    	//virtualGraph = new Graph();
    	
    	physicalCanvas = new GraphView(physicalGraph);
    	//virtualCanvas = new GraphView(virtualGraph);
    	
		graph.add(physicalCanvas);
		//graph.add(virtualCanvas);
		contentPane.add(graph, BorderLayout.CENTER);
    }
    
    
    /** dialog opening */
    private void openAddPhysicalNodeDialog(){
    	AddPhysicalNode phyNode = new AddPhysicalNode(physicalGraph, FogGui.this);
    	physicalCanvas.repaint();
    }
    private void openAddPhysicalEdgeDialog(){
    	AddPhysicalEdge phyEdge = new AddPhysicalEdge(physicalGraph, FogGui.this);
    	physicalCanvas.repaint();
    }

    protected void openAddSensorDialog() {
		AddSensor sensor = new AddSensor(physicalGraph, FogGui.this);
		physicalCanvas.repaint();
	}
    
    /** common utility */
    private String importFile(String type){
        JFileChooser fileopen = new JFileChooser();
        FileFilter filter = new FileNameExtensionFilter(type.toUpperCase()+" Files", type);
        fileopen.addChoosableFileFilter(filter);

        int ret = fileopen.showDialog(panel, "Import file");

        if (ret == JFileChooser.APPROVE_OPTION) {
            File file = fileopen.getSelectedFile();
            return file.getPath();
        }
        return "";
    }
    
    /** save network topology */
    private void saveFile(String type, Graph graph) throws IOException{
    	JFileChooser fileopen = new JFileChooser();
        FileFilter filter = new FileNameExtensionFilter(type.toUpperCase()+" Files", type);
        fileopen.addChoosableFileFilter(filter);

        int ret = fileopen.showSaveDialog(panel);

        if (ret == JFileChooser.APPROVE_OPTION) {
        	String jsonText = graph.toJsonString();
        	System.out.println(jsonText);
            String path = fileopen.getSelectedFile().toString();
            File file = new File(path);
    		FileOutputStream out = new FileOutputStream(file);
			out.write(jsonText.getBytes());
			out.close();
        }
    }
    
    private static void setUIFont(javax.swing.plaf.FontUIResource f){
        java.util.Enumeration keys = UIManager.getDefaults().keys();
        while (keys.hasMoreElements()) {
          Object key = keys.nextElement();
          Object value = UIManager.get (key);
          if (value != null && value instanceof javax.swing.plaf.FontUIResource)
            UIManager.put (key, f);
          }
    }
    
    private void checkImportStatus(){
    	if((physicalTopologyFile!=null && !physicalTopologyFile.isEmpty()) &&
    	   (deploymentFile!=null && !deploymentFile.isEmpty()) &&
           (workloads_background!=null && !workloads_background.isEmpty()) &&
    	   (workloads!=null && !workloads.isEmpty())){
    		btnRun.setEnabled(true);
    	}else{
    		btnRun.setEnabled(false);
    	}
    }
    
    
    
    /** Application entry point */
	public static void main(String args[]) throws InterruptedException {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
            	FogGui sdn = new FogGui();
                sdn.setVisible(true);
            }
        });
	}
}
