package org.skylion.mangareader.util;

import java.awt.AWTException;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Insets;
import java.awt.MenuContainer;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Robot;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.URL;

import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

/**
 * ImagePanel
 * This class extends JLabel allowing zooming and panning of an image contained within a JLabel.
 * @author Aaron Gokaslan (Skylion)
 */
public class ImagePanel extends JLabel 
implements MenuContainer, Serializable, SwingConstants {//Applicable interfaces 

	/**
	 * Auto-generated serial long
	 */
	private static final long serialVersionUID = 5026409875485764366L;

	private BufferedImage image;  //The image you want to handle
	private double scale;  //Scale factor
	private double x = 0, y = 0; //The image origin.
	private double startX, startY; //Temporary instance data used for panning
	private boolean isZoomed = false;//Determine is isZoomed

	/**
	 * Constructor
	 */
	public ImagePanel(){
		this((BufferedImage)null);
	}

	/**
	 * Constructor with Text
	 * @param text
	 */
	public ImagePanel(String text) {
		this();
		setText(text);
	}

	@Deprecated
	public ImagePanel(Icon icon){
		this(extractImageFromIcon(icon));
	}

	/**
	 * Constructor
	 * @param text
	 * @param HorizontalAlignment
	 */
	public ImagePanel(String text, int HorizontalAlignment) {
		this();
		setText(text);
		this.setHorizontalAlignment(HorizontalAlignment);
	}

	@Deprecated
	public ImagePanel(Icon image, int HorizontalAlignment) {
		this(image);
		this.setHorizontalAlignment(HorizontalAlignment);
	}

	@Deprecated
	public ImagePanel(String text, Icon image, int HorizontalAlignment) {
		this(image);
		setText(text);
		this.setHorizontalAlignment(HorizontalAlignment);
	}


	/**
	 * Constructor
	 * @param file to get read the buffered image from.
	 * @throws IOException if it cannot read the image
	 */
	public ImagePanel(File file) throws IOException{
		this(ImageIO.read(file));
	}

	/**
	 * Constructor
	 * @param url reads the image from URL.
	 * @throws IOException if something goes wrong.
	 */
	public ImagePanel(URL url) throws IOException{
		this(ImageIO.read(url));
	}

	public ImagePanel(BufferedImage image)  
	{  
		super();
		scale = 1.0;  
		loadImage(image);
		this.setOpaque(true);//Required for background to display
		addMouseListeners();
		this.setDoubleBuffered(true);
		startX = startY = x = y = 0;
	}  	

	/**
	 * Adds mouse listeners to the image panel
	 */
	private void addMouseListeners(){
		this.addMouseListener(new MouseAdapter(){
			@Override
			public void mousePressed(MouseEvent me){
				if(getImage()==null){
					return;
				}
				if(SwingUtilities.isRightMouseButton(me)){//Zooms in and out
					if(!isZoomed){
						setScale(scale*1.5);
						double dx = me.getX() - getWidth()/2;
						double dy = me.getY() - getHeight()/2;
						dx*=getZoomFactor();
						dy*=getZoomFactor();
						x-=dx;
						y-=dy;
						setDoubleBuffered(false);
						isZoomed = true;
					}
					else{
//						Point p = me.getPoint();
//						p.translate(-getWidth()/2, -getHeight()/2);
//						System.out.println(p);
						setScale(getPreferredScale());
						setDoubleBuffered(true);
						x = y = 0;
						isZoomed = false;
					}
				}
				else if(isZoomed && SwingUtilities.isLeftMouseButton(me)){//Begins to zoom
					super.mousePressed(me);
					//Changes mouse cursor
					ImagePanel.this.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
					
					//Gets instance data
					startX = me.getX();
					startY = me.getY();
				}
			}

			public void mouseReleased(MouseEvent me){
				ImagePanel.this.setCursor(Cursor.getDefaultCursor());//Returns cursor
			}
			
		});
		this.addMouseMotionListener(new MouseAdapter(){//This listener handles panning.
			@Override
			public void mouseDragged(MouseEvent me){
				if(scale!=getPreferredScale() && SwingUtilities.isLeftMouseButton(me) 
						&& getImage()!=null){
					setDoubleBuffered(false);
					
					//Calculates motion + acceleration
					double xDrift = me.getX() - startX;
					double yDrift = me.getY() - startY;
					
					if(isXValid(x+xDrift)){
						x+=xDrift;
						startX = me.getX();
					}
					
					if(isYValid(y+yDrift)){
						y+=yDrift;	
						startY = me.getY();
					}					
					
					repaint();
					setDoubleBuffered(true);
				}
			}
			
		});
		this.addMouseWheelListener(new MouseAdapter(){//This listener zooms using mouse wheel
			@Override
			public void mouseWheelMoved(MouseWheelEvent me){
				if(getImage() == null){
					return;
				}
				double scaleChange = me.getWheelRotation()/100d;
				if(!(getZoomFactor()<=.25 && scaleChange<0)){
					setScale(getScale() + scaleChange);
				}
				double dx = me.getX() - getWidth()/2;
				double dy = me.getY() - getHeight()/2;
				dx/=getZoomFactor();
				dy/=getZoomFactor();
				if(!isXValid(x-dx) || !isYValid(y-dx)){
					return;
				}
				x-=dx;
				y-=dy;
				try {//Makes the origin reset only happen once.
					Robot bot = new Robot();
					Point loc = getLocationOnScreen();
					loc.translate(getWidth()/2, getHeight()/2);
					bot.mouseMove(loc.x, loc.y);
				} catch (AWTException e) {
					e.printStackTrace();
				}
			}			
		});
		this.addComponentListener(new ComponentAdapter(){//Resizes image when component resizes
			@Override
			public void componentResized(ComponentEvent ce){
				if(!isZoomed){
					setScale(getPreferredScale());
				}
			}
		});

	}

	/**
	 * Determines if the value + the x value will be valid
	 * @param x The value you want to pan to.
	 * @return True if valid else false;
	 */
	private boolean isXValid(double x){
		BufferedImage image = getImage();
		double xImageCenter = getX() + image.getWidth()/2;
		return x+xImageCenter>=image.getMinX() && x+xImageCenter<=image.getWidth();
	}
	
	/**
	 * Determines if the value + the y value will be valid
	 * @param y The value you want to pan to.
	 * @return True if valid else false;
	 */
	private boolean isYValid(double y){
		BufferedImage image = getImage();
		double yImageCenter = getY() + image.getHeight()/2;
		return y+yImageCenter>=image.getMinY() && y+yImageCenter<=image.getHeight();
	}

	
	@Override
	protected void paintComponent(Graphics g){  
		if(image == null){//Does nothing special if no image is specified
			super.paintComponent(g);
			return;
		}
		BufferedImage image = this.image;//Prevents the image from being painted twice.
		this.image = null;
		Graphics2D g2 = (Graphics2D)g;  
		setRenderingHints(g2);
		super.paintComponent(g2);  
		this.image = image;
		int w = getWidth();  
		int h = getHeight();  
		int imageWidth = image.getWidth();  
		int imageHeight = image.getHeight();  
		double x = (w - scale * imageWidth)/2;  
		double y = (h - scale * imageHeight)/2;  
		AffineTransform at = AffineTransform.getTranslateInstance(x,y);  
		at.scale(scale, scale);  
		at.translate(this.x, this.y);
		g2.drawRenderedImage(image, at);  
	}  

	/**
	 * Sets Rendering Hints for the graphics object
	 * @param g2 The Graphics2D to apply the rendering hints to
	 */
	private void setRenderingHints(Graphics2D g2){
		g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);  
		g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
	}

	/**
	 * Sets the PreferredSize by modifying the image scale
	 */
	@Override
	public void setPreferredSize(Dimension d){
		if(image == null){
			super.setPreferredSize(d);
		}
		Dimension prefSize = this.getPreferredSize();
		int h = (int)(d.height/(double)prefSize.height+.5);
		int w = (int)(d.width/(double)prefSize.width+.5);
		if(h>w){
			setScale(h*scale);
		}
		else{
			setScale(w*scale);
		}
	}

	/**
	 * Sets the scale of the image
	 * @param The new scale in percent that you want to set the iage to
	 */
	protected void setScale(double s)  {
		if(s<=0){
			return;
		}
		scale = s;  
		repaint();  
	}

	/**
	 * Overriden to convert icon into images.
	 */
	@Deprecated
	@Override
	public void setIcon(Icon icon){
		loadImage(extractImageFromIcon(icon));
	}

	/**
	 * Returns null to prevent painting issues.
	 * Use get image instead.
	 */
	@Deprecated
	@Override
	public Icon getIcon(){
		if(image!=null){
			return new ImageIcon(image);
		}
		return null;//Must return null for painting sake
	}

	/**
	 * @return The current Scale factor (from the image's actual pixel size)
	 */
	protected double getScale(){
		return scale;
	}

	/**
	 * Return the preferred size based off of image scale (-1
	 */
	@Override
	public Dimension getPreferredSize()  
	{  
		if(image == null){
			return super.getPreferredSize();
		}
		int w = (int)(scale * image.getWidth());  
		int h = (int)(scale * image.getHeight());  
		return new Dimension(w, h);  
	}

	/**
	 * @return Current image in image panel
	 */
	public BufferedImage getImage(){
		return image;
	}

	/**
	 * Sets the offset of the image from the Center
	 * @param x The image offset
	 * @param y The image offset
	 */
	protected void setImageOffset(int x, int y){
		this.x = x;
		this.y = y;
		repaint();
	}
	/**
	 * Loads the image
	 * @param image The current image
	 */
	public void loadImage(BufferedImage image){
		this.image = image;
		if(image == null){
			return;
		}
		setScale(this.getPreferredScale(image));
		x = y = 0;
		repaint();
	}

	/**
	 * Calculates the ideal zoom factor to make the image fit into the JLabel
	 * @return the ideal zoom factor based off of the image
	 */
	protected double getPreferredScale(){
		return getPreferredScale(image);
	}
	
	/**
	 * Calculates the current zoom factor (eg. X0.5, X2, X3, X4...)
	 * @return the current zoom factor as a double.
	 */
	public double getZoomFactor(){
		return getScale()/getPreferredScale();
	}

	/**
	 * Calculates the preferredScale based of the specified image
	 * @param The image to calculate the preferred scale from
	 * @return The preferred scale or negative 1 the image is null.
	 */
	protected double getPreferredScale(BufferedImage image){
		if(image == null){
			return -1;
		}
		int ih = image.getHeight();
		int iw = image.getWidth();
		Insets insets = this.getInsets();
		int h, w;
		double scale = 1.0;

		if(this.getHeight() > 0 && this.getWidth() > 0){
			h =  this.getHeight() - insets.top - insets.bottom;
			w =  this.getWidth() - insets.left - insets.right;
		} else{
			h = this.getPreferredSize().height - insets.top - insets.bottom;
			w = this.getPreferredSize().width - insets.left - insets.right;
		}

		if(h/(double)w<ih/(double)iw){
			scale *= h/(double)ih;
		}
		else{
			scale *= w/(double)iw;
		}
		return scale;
	}

	/**
	 * Converts an image to a BufferedImage
	 * @param The image to extract the BufferedIMage
	 * @return The extracted BufferedImage
	 */
	private static BufferedImage convertToBufferedImage(Image image){
		if(image instanceof sun.awt.image.ToolkitImage){
			return ((sun.awt.image.ToolkitImage) image).getBufferedImage();
		}
		else if(image instanceof Image){
			return (BufferedImage)image;
		}
		else{
			return null;
		}
	}

	/**
	 * Extracts a BufferedImage from an Icon
	 * @param icon The icon to extract the image from
	 * @return The extracted BufferedImage
	 */
	private static BufferedImage extractImageFromIcon(Icon icon){
		if(icon == null || !(icon instanceof ImageIcon)){
			return null;
		}
		Image image = (Image)((ImageIcon)icon).getImage();
		if(image == null){
			return null;
		}
		return convertToBufferedImage(image);
	}
}


