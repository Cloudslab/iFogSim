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
public class AddPhysicalEdge extends JDialog {
	private static final long serialVersionUID = 4794808969864918000L;
	
	private final Graph graph;
	private JComboBox sourceNode;
	private JComboBox targetNode;
	private JTextField tfLatency;


	public AddPhysicalEdge(final Graph graph, final JFrame frame) {

		this.graph = graph;

		setLayout(new BorderLayout());

		add(createInputPanel(), BorderLayout.CENTER);
		add(createButtonPanel(), BorderLayout.PAGE_END);
		// show dialog
		setTitle("Add Physical Topology edge");
		setModal(true);
		setPreferredSize(new Dimension(400, 200));
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
		inputPanel.add(new Label("    ��"));
		inputPanel.add(targetNode);
		inputPanel.add(Box.createHorizontalGlue());
		inputPanelWrapper.add(inputPanel);

		textAreaPanel.add(Box.createRigidArea(new Dimension(10, 0)));
		textAreaPanel.add(new JLabel("Latency: "));
		tfLatency = new JTextField();
		tfLatency.setMaximumSize(tfLatency.getPreferredSize());
		tfLatency.setMinimumSize(new Dimension(150, tfLatency.getPreferredSize().height));
		tfLatency.setPreferredSize(new Dimension(150, tfLatency.getPreferredSize().height));

		textAreaPanel.add(tfLatency);
		textAreaPanel.add(Box.createHorizontalGlue());

		inputPanelWrapper.add(textAreaPanel);
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

				double latency = 0;
				boolean catchedError = false;

				if (tfLatency.getText() == null || tfLatency.getText().isEmpty()) {
					catchedError = true;
					prompt("Please type latency", "Error");
				}else {
					try {
						latency = Double.valueOf(tfLatency.getText());											
					} catch (NumberFormatException e1) {
						catchedError = true;
						prompt("Latency should be double type", "Error");
					}
				}

				if (!catchedError) {
					if (sourceNode.getSelectedItem() == null || targetNode.getSelectedItem() == null) {
						prompt("Please select node", "Error");
					} else {

						Node source = (Node) sourceNode.getSelectedItem();
						Node target = (Node) targetNode.getSelectedItem();

						Edge edge = new Edge(target, latency);
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
		JOptionPane.showMessageDialog(AddPhysicalEdge.this, msg, type, JOptionPane.ERROR_MESSAGE);
	}

}
