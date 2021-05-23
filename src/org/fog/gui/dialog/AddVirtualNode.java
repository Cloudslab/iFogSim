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
import org.fog.gui.core.SpringUtilities;
import org.fog.gui.core.VmNode;
import org.fog.gui.core.Node;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class AddVirtualNode extends JDialog {
	private static final long serialVersionUID = -5116677861770319577L;
	
	private final Graph graph;
	
	private JTextField tfName;
	private JComboBox cType;
	private JTextField tfSize;
	private JTextField tfPes;
	private JTextField tfMips;
	private JTextField tfRam;

	/**
	 * Constructor.
	 * 
	 * @param frame the parent frame
	 */
	public AddVirtualNode(final Graph graph, final JFrame frame) {
		this.graph = graph;
		
		setLayout(new BorderLayout());

		add(createInputPanelArea(), BorderLayout.CENTER);
		add(createButtonPanel(), BorderLayout.PAGE_END);
		// show dialog
		setTitle("Add Virtual Node");
		setModal(true);
		setPreferredSize(new Dimension(350, 400));
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
				} else if (cType.getSelectedIndex() < 0) {
					prompt("Please select VM type", "Error");
				} else if (tfSize.getText() == null || tfSize.getText().length() < 1) {
					prompt("Please type VM size", "Error");
				} else if (tfPes.getText() == null || tfPes.getText().length() < 1) {
					prompt("Please type pes", "Error");
				} else if (tfMips.getText() == null || tfMips.getText().length() < 1) {
					prompt("Please type VM mips", "Error");
				} else if (tfRam.getText() == null || tfRam.getText().length() < 1) {
					prompt("Please type VM RAM", "Error");
				} else {
					long t1 = 0;
					int t2 = 0;
					long t3 = 0;
					int t4 = 0;
					try {
						t1 = Long.parseLong(tfSize.getText());
						t2 = Integer.parseInt(tfPes.getText());
						t3 = Long.parseLong(tfMips.getText());
						t4 = Integer.parseInt(tfRam.getText());
					} catch (NumberFormatException e1) {
						catchedError = true;
						prompt("Input should be numerical character", "Error");
					}
					if(!catchedError){
						Node node = new VmNode(tfName.getText().toString(), (String)cType.getSelectedItem(),
										t1, t2, t3, t4);
						graph.addNode(node);
						setVisible(false);
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

	private JPanel createInputPanelArea() {
	    String[] vmType = {"vm"};
 
        //Create and populate the panel.
        JPanel springPanel = new JPanel(new SpringLayout());
        springPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
		
		JLabel lName = new JLabel("Name: ");
		springPanel.add(lName);
		tfName = new JTextField();
		lName.setLabelFor(tfName);
		springPanel.add(tfName);
		
		JLabel lType = new JLabel("Type: ", JLabel.TRAILING);
		springPanel.add(lType);	
		cType = new JComboBox(vmType);
		lType.setLabelFor(cType);
		cType.setSelectedIndex(-1);
		cType.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				
			}
		});
		springPanel.add(cType);		
		
		JLabel lSize = new JLabel("Size: ");
		springPanel.add(lSize);	
		tfSize = new JTextField();
		lSize.setLabelFor(tfSize);
		springPanel.add(tfSize);		
		
		JLabel lPes = new JLabel("Pes: ");
		springPanel.add(lPes);	
		tfPes = new JTextField();
		lPes.setLabelFor(tfPes);
		springPanel.add(tfPes);	
		
		JLabel lMips = new JLabel("Mips: ");
		springPanel.add(lMips);	
		tfMips = new JTextField();
		lMips.setLabelFor(tfMips);
		springPanel.add(tfMips);		
		
		JLabel lRam = new JLabel("Ram: ");
		springPanel.add(lRam);
		tfRam = new JTextField();
		lRam.setLabelFor(tfRam);
		springPanel.add(tfRam);
				
       //Lay out the panel.
        SpringUtilities.makeCompactGrid(springPanel,
                                        6, 2,        //rows, columns
                                        6, 6,        //initX, initY
                                        6, 6);       //xPad, yPad
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
		JOptionPane.showMessageDialog(AddVirtualNode.this, msg, type, JOptionPane.ERROR_MESSAGE);
	}
}
