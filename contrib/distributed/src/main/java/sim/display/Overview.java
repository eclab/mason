package sim.display;
import sim.field.partitioning.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import sim.util.*;

public class Overview extends JComponent
	{
	private static final long serialVersionUID = 1L;

	public static final double SIZE_SCALING = 0.125;
	IntRect2D[] bounds = new IntRect2D[0];
	int outerX;
	int outerY;
	int outerWidth = 1;
	int outerHeight = 1;
	int current = -1;
	SimStateProxy proxy;
	
	ArrayList<Integer> selected = new ArrayList<Integer>();
	HashSet<Integer> tempSelected = new HashSet<Integer>();
    boolean dragging = false;
    
    int drag_start_point_x = 0;
    int drag_start_point_y = 0;
    int mouse_current_x = 0;
    int mouse_current_y = 0;
    
    
	public Overview(SimStateProxy proxy)
	{
		
	//initialize	
	for (int i :proxy.chosenNodePartitionList)
	{
		selected.add((Integer)i);
	}
	this.proxy = proxy;
	addMouseListener(new MouseAdapter()
		{
		public void mouseClicked(MouseEvent e)
			{
	int width = getBounds().width;
	int height = getBounds().height;
			for(int i = 0; i < bounds.length; i++)
				{
				double x = (bounds[i].ul().x - outerX) / (double)(outerWidth) * width;
				double y = (bounds[i].ul().y - outerY) / (double)(outerHeight) * height;
				double w = (bounds[i].br().x - bounds[i].ul().x) / (double)(outerWidth) * width;
				double h = (bounds[i].br().y - bounds[i].ul().y) / (double)(outerHeight) * height;
				int ex = e.getX();
				int ey = e.getY();
				if (ex >= x && ex < x + w &&
					ey >= y && ey < y + h) // found it
					{
					
					if (e.isShiftDown())
					{
						toggleProcessor(i);
						break;
					}
					else
					{
						singleSelect(i); 
						break;
					}
					}
				}
			}


		    public void mouseReleased(MouseEvent e)
		    {
		    	
		    	if (dragging == true)
		    	{
		    	
				System.out.println("add Dragged called");		    	
		    	addDraggedProcessors();		    	
		    	dragging = false;
		    	}
		    }  
		    
		    
			
			
			}
	);
		
	
	addMouseMotionListener(new MouseMotionAdapter()
	{
		
	    public void mouseDragged(MouseEvent e)
			{
				System.out.println("dragging");
				if (dragging == false) //first dragging
					{
					drag_start_point_x = e.getX();
					drag_start_point_y = e.getY();
					}
				
			    mouse_current_x = e.getX();
			    mouse_current_y = e.getY();
				
				dragging = true;
	int width = getBounds().width;
	int height = getBounds().height;
			for(int i = 0; i < bounds.length; i++)
				{
				double x = (bounds[i].ul().x - outerX) / (double)(outerWidth) * width;
				double y = (bounds[i].ul().y - outerY) / (double)(outerHeight) * height;
				double w = (bounds[i].br().x - bounds[i].ul().x) / (double)(outerWidth) * width;
				double h = (bounds[i].br().y - bounds[i].ul().y) / (double)(outerHeight) * height;

				
				double bound_ul_x = x;
				double bound_ul_y = y;
				double bound_br_x = x + w;
				double bound_br_y = y + h;
				
				
				double drag_rect_ul_x;
				double drag_rect_ul_y;
				double drag_rect_br_x;
				double drag_rect_br_y;
				
				if (drag_start_point_x < mouse_current_x)
				{
					drag_rect_ul_x = drag_start_point_x;
					drag_rect_br_x = mouse_current_x;
				}
				else
				{
					drag_rect_ul_x = mouse_current_x;
					drag_rect_br_x = drag_start_point_x;				
				}
				
				
				if (drag_start_point_y < mouse_current_y)
				{
					drag_rect_ul_y = drag_start_point_y;
					drag_rect_br_y = mouse_current_y;
				}
				else
				{
					drag_rect_ul_y = mouse_current_y;
					drag_rect_br_y = drag_start_point_y;				
				}
                
                //intersects
                if (bound_ul_x < drag_rect_br_x && bound_ul_y < drag_rect_br_y && drag_rect_ul_x < bound_br_x && drag_rect_ul_y < bound_br_y )
                {
					System.out.println("add to temp selected called");
					addToTempSelected(i); 
					//break;
				}
                
				}
			
			repaint();

			}

	    
		
		
		});
	}
	
	public void changeCurrentProcessor(int val)
		{
		setCurrent(val);
		proxy.setCurrentProcessor(val);
		repaint();
		}
	
	public void setCurrent(int current)
		{
		this.current = current;
		}

	
	public void singleSelect(int i)
	{
		int[] int_selected = {i};

		
		proxy.chosenNodePartitionList = int_selected;
		
		selected = new ArrayList<Integer>();
		selected.add((Integer)i);
		
		repaint();
	}
	
	public void toggleProcessor(int i)
	{
		if (selected.contains((Integer)i))
		{
			selected.remove((Integer)i);
		}
		else
		{
			selected.add((Integer)i);
		}
		
		int[] int_selected = new int[selected.size()];
		
		for (int q=0; q<int_selected.length; q++)
		{
			int_selected[q] = selected.get(q);
		}
		
		proxy.chosenNodePartitionList = int_selected;
		
		repaint();
	}
	
	public void addToTempSelected(int i)
	{
		tempSelected.add((Integer)i);
	}

	public void addDraggedProcessors()
	{
		selected = new ArrayList<Integer>(tempSelected);
		int[] int_selected = new int[tempSelected.size()];

		for (int i = 0; i<tempSelected.size() ; i++)
		{
			int_selected[i] = selected.get(i);
		}
		
		proxy.chosenNodePartitionList = int_selected;
		
		tempSelected = new HashSet<Integer>();

		repaint();
	}
		
	public void update(ArrayList<IntRect2D> b) 	// , int aoi)
		{
		IntRect2D[] bounds = b.toArray(new IntRect2D[0]);
		
		// strip off aoi
		//int[] extra = new int[] { -aoi, -aoi, -aoi, -aoi };
		for(int i = 0; i < bounds.length; i++)
			{
			// bounds[i] = bounds[i].resize(extra);
			if (i == 0 || bounds[i].ul().x < outerX) outerX = bounds[i].ul().x;
			if (i == 0 || bounds[i].ul().y < outerY) outerY = bounds[i].ul().y;
			if (i == 0 || bounds[i].br().x > outerWidth) outerWidth = bounds[i].br().x; 
			if (i == 0 || bounds[i].br().y > outerHeight) outerHeight = bounds[i].br().y;
			}
		outerWidth-=outerX;
		outerHeight-=outerY;
		// so we don't divide by zero
		if (outerWidth <= 0) outerWidth = 1;
		if (outerHeight <= 0) outerHeight = 1;
		this.bounds = bounds;
		}
	
	
	public void paintComponent(Graphics graphics)
		{
		Graphics2D g = (Graphics2D)graphics;
		int width = getBounds().width;
		int height = getBounds().height;
		
		g.setColor(Color.BLACK);
		g.fillRect(0, 0, width, height);

		int size = (int)((1.0 / Math.sqrt(bounds.length)) * Math.min(width, height) * SIZE_SCALING) + 1;
		Font font = new Font(Font.SANS_SERIF, Font.PLAIN, size);
		g.setFont(font);
		g.setColor(Color.WHITE);
		FontMetrics fm = g.getFontMetrics(font);
		int fmHeight = fm.getAscent();			// we only need ascent since we're doing numbers
		
		for (int i = 0; i < bounds.length; i++)
			{
			String str = "" + i;
					double x = (bounds[i].ul().x - outerX) / (double)(outerWidth) * width;
					double y = (bounds[i].ul().y - outerY) / (double)(outerHeight) * height;
					double w = (bounds[i].br().x - bounds[i].ul().x) / (double)(outerWidth) * width;
					double h = (bounds[i].br().y - bounds[i].ul().y) / (double)(outerHeight) * height;
			int fmWidth = fm.stringWidth(str);
			
			//if (current == i)
			if (selected.contains((Integer)i))
				{
				g.fillRect((int)x, (int)y, (int)w, (int)h);
				g.setColor(Color.BLACK);
				g.drawString(str, (float)(x + w * 0.5 - fmWidth * 0.5), (float)(y + h * 0.5 + fmHeight * 0.5));
				g.setColor(Color.WHITE);
				}
			else
				{
				g.drawRect((int)x, (int)y, (int)w, (int)h);
				g.drawString(str, (float)(x + w * 0.5 - fmWidth * 0.5), (float)(y + h * 0.5 + fmHeight * 0.5));
				}
			}
		
		if (dragging == true)
		{
			System.out.println("drawing drag box");
			g.setColor(Color.YELLOW);

            int ul_x;
            int br_x;
            int ul_y;
            int br_y;
			
			if (drag_start_point_x < mouse_current_x)
			{
				ul_x = drag_start_point_x;
				br_x = mouse_current_x;
			}
			else
			{
				ul_x = mouse_current_x;
				br_x = drag_start_point_x;
			}
			
			if (drag_start_point_y < mouse_current_y)
			{
                ul_y = drag_start_point_y;
                br_y = mouse_current_y;
			}
			else
			{
				ul_y = mouse_current_y;
				br_y = drag_start_point_y;
			}
			
			
			g.drawRect(ul_x, ul_y, br_x - ul_x, br_y - ul_y);

		}
		
		}
	}