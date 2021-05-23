package org.fog.gui.dialog;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SpringLayout;
import javax.swing.UIManager;

import org.fog.gui.core.Graph;
import org.fog.gui.core.Node;
import org.fog.gui.core.SpringUtilities;
import org.fog.gui.core.SwitchNode;
import org.fog.gui.core.HostNode;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class AddPhysicalNode extends JDialog {
	private static final long serialVersionUID = -5116677861770319577L;
	
	private final Graph graph;
	
	private JLabel lName;
	private JLabel lType;
	private JLabel lBw;
	private JLabel lop1;
	private JLabel lop2;
	private JLabel lop3;
	private JLabel lop4;
	
	private JTextField tfName;
	private JComboBox cType;
	private JTextField tfBw;
	private JTextField top1;
	private JTextField top2;
	private JTextField top3;
	private JTextField top4;


	public AddPhysicalNode(final Graph graph, final JFrame frame) {
		this.graph = graph;
		
		setLayout(new BorderLayout());

		add(createInputPanelArea(), BorderLayout.CENTER);
		add(createButtonPanel(), BorderLayout.PAGE_END);
		// show dialog
		setTitle("Add Physical Node");
		setModal(true);
		setPreferredSize(new Dimension(350, 380));
		setResizable(false);
		pack();
		setLocationRelativeTo(frame);
		setVisible(true);

	}

	private JPanel createButtonPanel() {

		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.LINE_AXIS));
		
		JButton okBtn = new JButton("Ok");
		JButton cancelBtn = new JButton("Cancel");
		
		cancelBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                setVisible(false);
            }
        });

		okBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				boolean catchedError = false;
				if (tfName.getText() == null || tfName.getText().length() < 1) {
					prompt("Please type VM name", "Error");
				} else if (tfBw.getText() == null || tfBw.getText().length() < 1) {
					prompt("Please type VM Bw", "Error");				
				} else if(cType.getSelectedIndex() < 0) {
					prompt("Please type VM Type", "Error");
				}else{
					String type = (String)cType.getSelectedItem();
					if("host"==getType(type)){
						if (top1.getText() == null || top1.getText().length() < 1) {
							prompt("Please type pes", "Error");	
						} else if (top2.getText() == null || top2.getText().length() < 1) {
							prompt("Please type VM mips", "Error");	
						} else if (top3.getText() == null || top3.getText().length() < 1) {
							prompt("Please type VM RAM", "Error");	
						} else if (top4.getText() == null || top4.getText().length() < 1) {
							prompt("Please type VM Storage", "Error");	
						} else {
							long t1 = 0;
							long t2 = 0;
							int t3 = 0;
							long t4 = 0;
							long t5 = 0;
							try {
								t1 = Long.parseLong(top1.getText());
								t2 = Long.parseLong(top2.getText());
								t3 = Integer.parseInt(top3.getText());
								t4 = Long.parseLong(top4.getText());
								t5 = Long.parseLong(tfBw.getText());
								catchedError = false;
							} catch (NumberFormatException e1) {
								catchedError = true;
								prompt("Input should be numerical character", "Error");
							}
							if(!catchedError){
								Node node = new HostNode(tfName.getText().toString(), type, t1, t2, t3, t4, t5);
								graph.addNode(node);
								setVisible(false);								
							}
						}
					}else if("switch"==getType(type)){
						if (top1.getText() == null || top1.getText().length() < 1) {
							prompt("Please type Iops", "Error");
						} else if (top2.getText() == null || top2.getText().length() < 1) {
							prompt("Please type upports", "Error");
						} else if (top3.getText() == null || top3.getText().length() < 1) {
							prompt("Please type VM downports", "Error");
						} else {
							long t1 = 0;
							int t2 = 0;
							int t3 = 0;
							long t4 = 0;
							try {
								t1 = Long.parseLong(top1.getText());
								t2 = Integer.parseInt(top2.getText());
								t3 = Integer.parseInt(top3.getText());
								t4 = Long.parseLong(tfBw.getText());
								catchedError = false;
							} catch (NumberFormatException e1) {
								catchedError = true;
								prompt("Input should be numerical character", "Error");
							}
							if(!catchedError){
								Node node = new SwitchNode(tfName.getText().toString(), type, t1, t2, t3, t4);
								graph.addNode(node);
								setVisible(false);
							}
						}
					}
				}
			}
		});

		buttonPanel.add(Box.createHorizontalGlue());
		buttonPanel.add(okBtn);
		buttonPanel.add(Box.createRigidArea(new Dimension(10, 0)));
		buttonPanel.add(cancelBtn);
		buttonPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

		return buttonPanel;
	}

	private void updatePanel(String type){
		switch(type){
			case "core":
			case "edge":
				lop1.setText("Iops: ");
				lop2.setText("Upports: ");
				lop3.setText("Downports: ");
				lop4.setVisible(false);
				
				top1.setText("");
				top2.setText("");
				top3.setText("");
				top4.setVisible(false);
				break;
		
			case "host":
				lop1.setText("Pes: ");
				lop2.setText("Mips: ");
				lop3.setText("Ram: ");
				lop4.setVisible(true);
				lop4.setText("Storage: ");
				
				top1.setText("");
				top2.setText("");
				top3.setText("");
				top4.setVisible(true);
				top4.setText("");
				break;
		}
	}
	private String getType(String type){
		if("core"==type || "edge"==type){
			return "switch"; 
		} else if("host"==type){
			return "host";
		}
		return "";
	}
	private JPanel createInputPanelArea() {
	    String[] vmType = {"core","edge","host"};
 
        //Create and populate the panel.
        JPanel springPanel = new JPanel(new SpringLayout());
        springPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));		
        
		lName = new JLabel("Name: ");
		springPanel.add(lName);
		tfName = new JTextField();
		lName.setLabelFor(tfName);
		springPanel.add(tfName);

		lType = new JLabel("Type: "); //, JLabel.TRAILING);
		springPanel.add(lType);	
		cType = new JComboBox(vmType);
		lType.setLabelFor(cType);
		cType.setSelectedIndex(0);
		cType.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				JComboBox ctype = (JComboBox)e.getSource();
				String item = (String)ctype.getSelectedItem();
				updatePanel(item);
			}
		});
		springPanel.add(cType);	
		
		lBw = new JLabel("Bw: ");
		springPanel.add(lBw);
		tfBw = new JTextField();
		lBw.setLabelFor(tfBw);
		springPanel.add(tfBw);
		
		/** switch and host  */
		lop1 = new JLabel("Pes: ");
		springPanel.add(lop1);	
		top1 = new JTextField();
		lop1.setLabelFor(top1);
		springPanel.add(top1);	
		
		lop2 = new JLabel("Mips: ");
		springPanel.add(lop2);	
		top2 = new JTextField();
		lop2.setLabelFor(top2);
		springPanel.add(top2);		
		
		lop3 = new JLabel("Ram: ");
		springPanel.add(lop3);
		top3 = new JTextField();
		lop3.setLabelFor(top3);
		springPanel.add(top3);
		
		lop4 = new JLabel("Storage: ");
		springPanel.add(lop4);	
		top4 = new JTextField();
		lop4.setLabelFor(top4);
		springPanel.add(top4);

		
        //Lay out the panel.
        SpringUtilities.makeCompactGrid(springPanel,
                                        7, 2,        //rows, cols
                                        6, 6,        //initX, initY
                                        6, 6);       //xPad, yPad
        updatePanel("core");
		return springPanel;
	}
	
    public static void setUIFont (javax.swing.plaf.FontUIResource f){
        java.util.Enumeration keys = UIManager.getDefaults().keys();
        while (keys.hasMoreElements()) {
          Object key = keys.nextElement();
          Object value = UIManager.get (key);
          if (value != null && value instanceof javax.swing.plaf.FontUIResource)
            UIManager.put (key, f);
          }
    } 
    
	private void prompt(String msg, String type){
		JOptionPane.showMessageDialog(AddPhysicalNode.this, msg, type, JOptionPane.ERROR_MESSAGE);
	}
}
