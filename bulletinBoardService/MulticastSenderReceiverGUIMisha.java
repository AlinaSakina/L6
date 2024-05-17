package bulletinBoardService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetAddress;

public class MulticastSenderReceiverGUIMisha extends JFrame {
    private JTextArea textArea;
    private JTextField textFieldMsg;
    private JTextField textFieldGroup;
    private JTextField textFieldPort;
    private JTextField textFieldName;
    private JButton connectButton;
    private JButton disconnectButton;
    private JButton clearButton;
    private JButton exitButton;
    private JButton sendButton;
    private Messanger messanger;

    public MulticastSenderReceiverGUIMisha() {
        setTitle("Multicast Bulletin Board");
        setSize(600, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel topPanel = new JPanel(new BorderLayout());
        textFieldMsg = new JTextField();
        textFieldMsg.setColumns(20); 
        topPanel.add(textFieldMsg, BorderLayout.CENTER);
        topPanel.setBorder(new EmptyBorder(10, 10, 10, 10)); 
        sendButton = new JButton("Send");
        sendButton.setPreferredSize(new Dimension(100, 30)); 
        topPanel.add(sendButton, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        textArea = new JTextArea();
        textArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(400, 200)); 
        add(scrollPane, BorderLayout.CENTER);

        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS)); 
        leftPanel.setPreferredSize(new Dimension(150, getHeight()));
        leftPanel.setBorder(new EmptyBorder(15, 15, 15, 15)); 
        leftPanel.add(new JLabel("Group:"));
        textFieldGroup = new JTextField("224.0.0.1");
        textFieldGroup.setColumns(8); 
        textFieldGroup.setPreferredSize(new Dimension(textFieldGroup.getPreferredSize().width, textFieldGroup.getPreferredSize().height));
        leftPanel.add(textFieldGroup);
        leftPanel.add(Box.createRigidArea(new Dimension(0, 5))); 
        leftPanel.add(new JLabel("Port:"));
        textFieldPort = new JTextField("3456");
        textFieldPort.setColumns(4); 
        textFieldGroup.setPreferredSize(new Dimension(textFieldGroup.getPreferredSize().width, textFieldGroup.getPreferredSize().height));
        leftPanel.add(textFieldPort);
        leftPanel.add(Box.createRigidArea(new Dimension(0, 5))); 
        leftPanel.add(new JLabel("Name:"));
        textFieldName = new JTextField();
        textFieldName.setColumns(8); 
        textFieldGroup.setPreferredSize(new Dimension(textFieldGroup.getPreferredSize().width, textFieldGroup.getPreferredSize().height));
        leftPanel.add(textFieldName);
        add(leftPanel, BorderLayout.WEST);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(1, 4, 5, 5)); 
        buttonPanel.setPreferredSize(new Dimension(getWidth(), 50));
        buttonPanel.setBorder(new EmptyBorder(10, 10, 10, 10)); 
        connectButton = new JButton("Connect");
        disconnectButton = new JButton("Disconnect");
        clearButton = new JButton("Clear");
        exitButton = new JButton("Exit");
        buttonPanel.add(connectButton);
        buttonPanel.add(disconnectButton);
        buttonPanel.add(clearButton);
        buttonPanel.add(exitButton);
        add(buttonPanel, BorderLayout.SOUTH);

        connectButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                connect();
            }
        });

        disconnectButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                disconnect();
            }
        });

        clearButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                clear();
            }
        });

        exitButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                exit();
            }
        });

        sendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });

        setVisible(true);
    }

    private void connect() {
        try {
            InetAddress addr = InetAddress.getByName(textFieldGroup.getText());
            int port = Integer.parseInt(textFieldPort.getText());
            String name = textFieldName.getText();
            if (name == null || name.trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Name cannot be empty", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            name = name.trim();
            UITasks ui = (UITasks) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class[]{UITasks.class},
                    new EDTInvocationHandler(new UITasksImpl()));
            messanger = new MessangerImpl(addr, port, name, ui);
            messanger.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void disconnect() {
        if (messanger != null) {
            messanger.stop();
            messanger = null;
        }
    }

    private void clear() {
        textArea.setText("");
    }

    private void exit() {
        disconnect();
        System.exit(0);
    }

    private void sendMessage() {
        if (messanger != null) {
            messanger.send();
        }
    }

    private class EDTInvocationHandler implements InvocationHandler {
        private Object invocationResult = null;
        private UITasks ui;

        public EDTInvocationHandler(UITasks ui) {
            this.ui = ui;
        }

        @Override
        public Object invoke(Object proxy, final Method method, final Object[] args) throws Throwable {
            if (SwingUtilities.isEventDispatchThread()) {
                invocationResult = method.invoke(ui, args);
            } else {
                Runnable shell = new Runnable() {
                    @Override
                    public void run() {
                        try {
                            invocationResult = method.invoke(ui, args);
                        } catch (Exception ex) {
                            throw new RuntimeException(ex);
                        }
                    }
                };
                SwingUtilities.invokeAndWait(shell);
            }
            return invocationResult;
        }
    }
    
    private class UITasksImpl implements UITasks {
        @Override
        public String getMessage() {
            String res = textFieldMsg.getText();
            textFieldMsg.setText("");
            return res;
        }
    
        @Override
        public void setText(String txt) {
            textArea.append(txt + "\n");
        }
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(MulticastSenderReceiverGUI::new);
    }
}

interface Messanger {
    void start();
    void stop();
    void send();
}

interface UITasks {
    String getMessage();
    void setText(String txt);
}
