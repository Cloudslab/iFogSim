package org.fog.gui.dialog;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Label;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.fog.gui.core.Edge;
import org.fog.gui.core.Graph;
import org.fog.gui.core.Node;
import org.fog.gui.core.NodeCellRenderer;



/** A dialog to add a new edge */
public class AddVirtualEdge extends JDialog {
	private static final long serialVersionUID = 4794808969864918000L;
	
	private final Graph graph;
	private JComboBox sourceNode;
	private JComboBox targetNode;
	private JTextField tfName;
	private JTextField tfBandwidth;


	public AddVirtualEdge(final Graph graph, final JFrame frame) {

		this.graph = graph;

		setLayout(new BorderLayout());

		add(createInputPanel(), BorderLayout.CENTER);
		add(createButtonPanel(), BorderLayout.PAGE_END);
		// show dialog
		setTitle("Add Virtual Topology edge");
		setModal(true);
		setPreferredSize(new Dimension(400, 250));
		setResizable(false);
		pack();
		setLocationRelativeTo(frame); // must be called between pack and setVisible to work properly
		setVisible(true);
	}

	@SuppressWarnings("unchecked")
	private JPanel createInputPanel() {

		Component rigid = Box.createRigidArea(new Dimension(10, 0));

		JPanel inputPanelWrapper = new JPanel();
		inputPanelWrapper.setLayout(new BoxLayout(inputPanelWrapper, BoxLayout.PAGE_AXIS));

		JPanel inputPanel = new JPanel();
		inputPanel.setLayout(new BoxLayout(inputPanel, BoxLayout.LINE_AXIS));

		JPanel textAreaPanel = new JPanel();
		textAreaPanel.setLayout(new BoxLayout(textAreaPanel, BoxLayout.LINE_AXIS));
		
		JPanel textAreaPanel2 = new JPanel();
		textAreaPanel2.setLayout(new BoxLayout(textAreaPanel2, BoxLayout.LINE_AXIS));

		ComboBoxModel sourceNodeModel = new DefaultComboBoxModel(graph.getAdjacencyList().keySet().toArray());

		sourceNodeModel.setSelectedItem(null);

		sourceNode = new JComboBox(sourceNodeModel);
		targetNode = new JComboBox();
		sourceNode.setMaximumSize(sourceNode.getPreferredSize());
		sourceNode.setMinimumSize(new Dimension(150, sourceNode.getPreferredSize().height));
		sourceNode.setPreferredSize(new Dimension(150, sourceNode.getPreferredSize().height));
		targetNode.setMaximumSize(targetNode.getPreferredSize());
		targetNode.setMinimumSize(new Dimension(150, targetNode.getPreferredSize().height));
		targetNode.setPreferredSize(new Dimension(150, targetNode.getPreferredSize().height));

		NodeCellRenderer renderer = new NodeCellRenderer();

		sourceNode.setRenderer(renderer);
		targetNode.setRenderer(renderer);

		sourceNode.addItemListener(new ItemListener() {

			@Override
			public void itemStateChanged(ItemEvent e) {
				// only display nodes which do not have already an edge

				targetNode.removeAllItems();
				Node selectedNode = (Node) sourceNode.getSelectedItem();

				if (selectedNode != null) {

					List<Node> nodesToDisplay = new ArrayList<Node>();
					Set<Node> allNodes = graph.getAdjacencyList().keySet();

					// get edged for selected node and throw out all target nodes where already an edge exists
					List<Edge> edgesForSelectedNode = graph.getAdjacencyList().get(selectedNode);
					Set<Node> nodesInEdges = new HashSet<Node>();
					for (Edge edge : edgesForSelectedNode) {
						nodesInEdges.add(edge.getNode());
					}

					for (Node node : allNodes) {
						if (!node.equals(selectedNode) && !nodesInEdges.contains(node)) {
							nodesToDisplay.add(node);
						}
					}

					ComboBoxModel targetNodeModel = new DefaultComboBoxModel(nodesToDisplay.toArray());
					targetNode.setModel(targetNodeModel);
				}
			}
		});

		inputPanel.add(sourceNode);
		// inputPanel.add(Box.createRigidArea(new Dimension(10, 0)));
		inputPanel.add(new Label("    ��"));
		inputPanel.add(targetNode);
		inputPanel.add(Box.createHorizontalGlue());
		inputPanelWrapper.add(inputPanel);

		textAreaPanel.add(Box.createRigidArea(new Dimension(10, 0)));
		textAreaPanel.add(new JLabel("Edge Name: "));
		tfName = new JTextField();
		tfName.setMaximumSize(tfName.getPreferredSize());
		tfName.setMinimumSize(new Dimension(180, tfName.getPreferredSize().height));
		tfName.setPreferredSize(new Dimension(180, tfName.getPreferredSize().height));
		textAreaPanel.add(tfName);
		textAreaPanel.add(Box.createHorizontalGlue());
		inputPanelWrapper.add(textAreaPanel);
		inputPanelWrapper.add(Box.createVerticalGlue());
		
		textAreaPanel2.add(Box.createRigidArea(new Dimension(10, 0)));
		textAreaPanel2.add(new JLabel("Bandwidth:  "));
		tfBandwidth = new JTextField();
		tfBandwidth.setMaximumSize(tfBandwidth.getPreferredSize());
		tfBandwidth.setMinimumSize(new Dimension(180, tfBandwidth.getPreferredSize().height));
		tfBandwidth.setPreferredSize(new Dimension(180, tfBandwidth.getPreferredSize().height));
		textAreaPanel2.add(tfBandwidth);
		textAreaPanel2.add(Box.createHorizontalGlue());
		inputPanelWrapper.add(textAreaPanel2);
		inputPanelWrapper.add(Box.createVerticalGlue());

		inputPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		return inputPanelWrapper;
	}

	private JPanel createButtonPanel() {

		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.LINE_AXIS));

		JButton okBtn = new JButton("Ok");
		JButton cancelBtn = new JButton("Cancel");

		cancelBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				setVisible(false);
			}
		});

		okBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {

				String name = "default";
				long bandwidth = 0;
				boolean catchedError = false;
				
				if (tfName.getText() == null || tfName.getText().isEmpty()) {
					catchedError = true;
					prompt("Please type Edge Name", "Error");
				}else {
					name = (String)tfName.getText();								
				}

				if (tfBandwidth.getText() == null || tfBandwidth.getText().isEmpty()) {
					catchedError = true;
					prompt("Please type Bandwidth", "Error");
				}else {
					try {
						bandwidth = Long.valueOf(tfBandwidth.getText());											
					} catch (NumberFormatException e1) {
						catchedError = true;
						prompt("Bandwidth should be long type", "Error");
					}
				}

				if (!catchedError) {
					if (sourceNode.getSelectedItem() == null || targetNode.getSelectedItem() == null) {
						prompt("Please select node", "Error");
					} else {

						Node source = (Node) sourceNode.getSelectedItem();
						Node target = (Node) targetNode.getSelectedItem();

						Edge edge = new Edge(target, name, bandwidth);
						graph.addEdge(source, edge);

						setVisible(false);
					}
				}

			}
		});

		buttonPanel.add(Box.createHorizontalGlue());
		buttonPanel.add(okBtn);
		buttonPanel.add(Box.createRigidArea(new Dimension(10, 0)));
		buttonPanel.add(cancelBtn);
		buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		return buttonPanel;
	}
	
	private void prompt(String msg, String type){
		JOptionPane.showMessageDialog(AddVirtualEdge.this, msg, type, JOptionPane.ERROR_MESSAGE);
	}

}
