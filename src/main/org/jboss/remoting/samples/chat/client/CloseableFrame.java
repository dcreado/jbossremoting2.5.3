package org.jboss.remoting.samples.chat.client;

import java.awt.*;
import javax.swing.*;

/**
 * <p>Title: Chat4</p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2003</p>
 * <p>Company: </p>
 * @author not attributable
 * @version 1.0
 */

public class CloseableFrame extends JFrame implements Closeable {
  private Closeable parent;

  public CloseableFrame(Closeable parent) {
    setParent(parent);
  }

  public CloseableFrame() {
    setParent(null);
  }

  public void notifyOnClose(Component c)
  {
    setVisible(false);
    if (parent != null)
    {
      parent.notifyOnClose(this);
    }
  }

  protected void setParent(Closeable parent)
  {
    this.parent = parent;
  }

  protected Closeable parent()
  {
    return parent;
  }

  protected void center()
  {
    //Center the window
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    Dimension frameSize = getSize();
    if (frameSize.height > screenSize.height) {
      frameSize.height = screenSize.height;
    }
    if (frameSize.width > screenSize.width) {
      frameSize.width = screenSize.width;
    }
    setLocation( (screenSize.width - frameSize.width) / 2,
                      (screenSize.height - frameSize.height) / 2);
  }

  public static void main(String[] args) {
    Closeable CloseableFrame = new CloseableFrame();
  }
}