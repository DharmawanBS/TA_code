/*
 * SimGUI.java
 *
 * Created on April 27, 2007, 11:04 AM
 */

package sidnet.core.gui;

import java.awt.Color;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import jist.runtime.JistAPI;
import javax.swing.JProgressBar;

import sidnet.core.gui.animations.AnimationDrawingLayer;
import sidnet.core.gui.staticdrawing.StaticDrawingLayer;

        
/**
 *
 * @author  Oliver
 */
public class SimGUI extends javax.swing.JFrame implements MouseListener, JistAPI.DoNotRewrite{
    private PanelContext sensorPanelContext;
    private PanelContext utilityPanelContext1;
    private PanelContext utilityPanelContext2;
    public AnimationDrawingLayer animationDrawingLayer;
    public StaticDrawingLayer staticDrawingLayer;
    private String Title = "SIDnet-SWANS v1.5.4";
    
    /** Creates new form SimGUI */
    public SimGUI() {
        initComponents();
        addWindowListener(new WindowAdapter(){
         public void windowClosing(WindowEvent e){
            JistAPI.end();
            System.exit(0);
        }
        });
        this.setTitle(Title);
        this.setBounds(0,0,950,700);
        this.setVisible(true); 
        
        jSensorFieldPanel.setBackground(Color.LIGHT_GRAY);
        //jUtilityPanel1.setSize(new Dimension(300,300));
        //jUtilityPanel2.setSize(new Dimension(300,300));
        sensorPanelContext = new PanelContext(jSensorFieldPanel, new JPopupMenu());       
        utilityPanelContext1 = new PanelContext(jUtilityPanel1, new JPopupMenu());
        utilityPanelContext2 = new PanelContext(jUtilityPanel2, new JPopupMenu());
       
        jSensorFieldPanel.setOpaque(true);
        jUtilityPanel1.setOpaque(true);
        jUtilityPanel2.setOpaque(true);
        
        animationDrawingLayer = new AnimationDrawingLayer();
        animationDrawingLayer.configureGUI(sensorPanelContext.getPanelGUI());
        
        staticDrawingLayer = new StaticDrawingLayer();
        staticDrawingLayer.configureGUI(sensorPanelContext.getPanelGUI());       
    }
    
    public void appendTitle(String additionalTitleInformation) {
        this.setTitle(this.getTitle() + " * " + additionalTitleInformation);
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    
    /* Returns the panel in which the nodes will be plot */
    public PanelContext getSensorsPanelContext()
    {
        return sensorPanelContext;
    }
    
    public PanelContext getUtilityPanelContext1()
    {
        return utilityPanelContext1;
    }
    
    public PanelContext getUtilityPanelContext2()
    {
        return utilityPanelContext2;
    }
    
    public JPanel getSimControlPanel()
    {
        return jPanel1;
    }
    
    public JProgressBar getProgressBar()
    {
        return jProgressBar1;
    }
   
    
    
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents()
    {
        jPopupMenu = new javax.swing.JPopupMenu();
        jSensorFieldPanel = new javax.swing.JPanel();
        jPanel1 = new javax.swing.JPanel();
        jProgressBar1 = new javax.swing.JProgressBar();
        jUtilityPanel2 = new javax.swing.JPanel();
        jUtilityPanel1 = new javax.swing.JPanel();

        setResizable(false);
        jSensorFieldPanel.setBackground(new java.awt.Color(153, 153, 153));
        jSensorFieldPanel.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        jSensorFieldPanel.setMinimumSize(new java.awt.Dimension(600, 600));
        jSensorFieldPanel.setPreferredSize(new java.awt.Dimension(600, 600));
        org.jdesktop.layout.GroupLayout jSensorFieldPanelLayout = new org.jdesktop.layout.GroupLayout(jSensorFieldPanel);
        jSensorFieldPanel.setLayout(jSensorFieldPanelLayout);
        jSensorFieldPanelLayout.setHorizontalGroup(
            jSensorFieldPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 598, Short.MAX_VALUE)
        );
        jSensorFieldPanelLayout.setVerticalGroup(
            jSensorFieldPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 598, Short.MAX_VALUE)
        );

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Simulation Control"));
        jPanel1.setName("Simulation Control");
        org.jdesktop.layout.GroupLayout jPanel1Layout = new org.jdesktop.layout.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 290, Short.MAX_VALUE)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 42, Short.MAX_VALUE)
        );

        jProgressBar1.setForeground(new java.awt.Color(0, 0, 180));
        jProgressBar1.setToolTipText("Progress Bar");
        jProgressBar1.setStringPainted(true);

        jUtilityPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder("Utility View 2"));
        jUtilityPanel2.setMaximumSize(new java.awt.Dimension(300, 300));
        jUtilityPanel2.setMinimumSize(new java.awt.Dimension(300, 300));
        jUtilityPanel2.setPreferredSize(new java.awt.Dimension(300, 300));
        org.jdesktop.layout.GroupLayout jUtilityPanel2Layout = new org.jdesktop.layout.GroupLayout(jUtilityPanel2);
        jUtilityPanel2.setLayout(jUtilityPanel2Layout);
        jUtilityPanel2Layout.setHorizontalGroup(
            jUtilityPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 284, Short.MAX_VALUE)
        );
        jUtilityPanel2Layout.setVerticalGroup(
            jUtilityPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 253, Short.MAX_VALUE)
        );

        jUtilityPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Utility View 1"));
        jUtilityPanel1.setMaximumSize(new java.awt.Dimension(300, 300));
        jUtilityPanel1.setMinimumSize(new java.awt.Dimension(300, 300));
        jUtilityPanel1.setPreferredSize(new java.awt.Dimension(300, 300));
        org.jdesktop.layout.GroupLayout jUtilityPanel1Layout = new org.jdesktop.layout.GroupLayout(jUtilityPanel1);
        jUtilityPanel1.setLayout(jUtilityPanel1Layout);
        jUtilityPanel1Layout.setHorizontalGroup(
            jUtilityPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 284, Short.MAX_VALUE)
        );
        jUtilityPanel1Layout.setVerticalGroup(
            jUtilityPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 249, Short.MAX_VALUE)
        );

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jSensorFieldPanel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jProgressBar1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 600, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(layout.createSequentialGroup()
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(jUtilityPanel2, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .add(jUtilityPanel1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                        .add(16, 16, 16))
                    .add(layout.createSequentialGroup()
                        .add(jPanel1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addContainerGap())))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING, false)
                    .add(layout.createSequentialGroup()
                        .add(jUtilityPanel1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 279, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jUtilityPanel2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 283, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .add(jPanel1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                    .add(layout.createSequentialGroup()
                        .add(jSensorFieldPanel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .add(19, 19, 19)
                        .add(jProgressBar1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 26, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents
    
    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPopupMenu jPopupMenu;
    private javax.swing.JProgressBar jProgressBar1;
    private javax.swing.JPanel jSensorFieldPanel;
    private javax.swing.JPanel jUtilityPanel1;
    private javax.swing.JPanel jUtilityPanel2;
    // End of variables declaration//GEN-END:variables
    
     public void mouseClicked(MouseEvent e) {   
         
     }
    
    public void mouseEntered(MouseEvent e) {}
    
    public void mouseExited(MouseEvent e) {}
    
    public void mousePressed(MouseEvent e) {}
    
    public void mouseReleased(MouseEvent e) {}
    
    public AnimationDrawingLayer getAnimationDrawingTool() {
    	return animationDrawingLayer;
    }
    
    public StaticDrawingLayer getStaticDrawingTool() {
    	return staticDrawingLayer;
    }
}
