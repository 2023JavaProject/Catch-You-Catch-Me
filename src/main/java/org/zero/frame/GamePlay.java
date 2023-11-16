package org.zero.frame;

import org.zero.common.CommonUtil;
import org.zero.db.DB;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;
import java.util.Vector;

import static org.zero.common.CommonUtil.*;
import static org.zero.db.ConnectionMgr.getConnection;


public class GamePlay extends JFrame {
    Image background = new ImageIcon(Main.class.getResource("/static/img/backGround.png")).getImage();
    Image red = new ImageIcon(Main.class.getResource("/static/img/red.png")).getImage();
    Image orange = new ImageIcon(Main.class.getResource("/static/img/orange.png")).getImage();
    Image yellow = new ImageIcon(Main.class.getResource("/static/img/yellow.png")).getImage();
    Image green = new ImageIcon(Main.class.getResource("/static/img/green.png")).getImage();
    Image blue = new ImageIcon(Main.class.getResource("/static/img/blue.png")).getImage();
    Image purple = new ImageIcon(Main.class.getResource("/static/img/purple.png")).getImage();
    Image pink = new ImageIcon(Main.class.getResource("/static/img/pink.png")).getImage();
    Image black = new ImageIcon(Main.class.getResource("/static/img/black.png")).getImage();
    Image erase = new ImageIcon(Main.class.getResource("/static/img/erase.png")).getImage();
    Image trash = new ImageIcon(Main.class.getResource("/static/img/trash.png")).getImage();
    private JPanel backgroundPanel;
    private Color currentColor = new Color(0, 0, 0);
    private int currentPenSize = 5; // 펜 굵기
    private int startX, startY; // 그림 그리기 시작 위치
    private Vector<Integer> vector = new Vector<Integer>();
    private int x1Temp, y1Temp;
    Image drawIcon[] = {red, orange, yellow, green, blue, purple, pink, black, erase, trash};
    private static JTextArea chatArea = new JTextArea();
    private static JTextField messageField;
    private static PrintWriter writer;
    private static String userName;
    public static Connection conn = null;
    public static Statement stmt = null;
    private static PreparedStatement ps = null;
    private static ResultSet rs = null;
    private JLabel categoryContentJL = null;// 현재 제시어
    private String currentTopic;// 현재 주제
    private int prevMax = 0; // 이전 최대 값
    public static int userCnt = 0;// 현재 유저 수
    //유저 이름 (임의의 값으로 초기화)
    ArrayList<String> nameArr = new ArrayList<>(
            Arrays.asList("노하은", "정선영", "이지수", "박화경")
    );
    private Thread p_display, t_display;
    private JLabel minute, second, w3;
    private int mm, ss, ms, t = 0;
    private String currentTime;// 현재 시간

    public GamePlay(String userName) {

        CommonUtil.settings(this);
        this.userName = userName;
        dropCurrentTopic();// 현재 주제 초기화
        backgroundPanel = CommonUtil.makeBackground(backgroundPanel, background);
        setTimer(this.backgroundPanel);// timer

        JPanel pancelP = new JPanel();
        pancelP.setBounds(40, 360, 470, 107);
        pancelP.setBackground(new Color(255, 255, 255));
        backgroundPanel.add(pancelP);

        DrawingPanel drawingPanel = new DrawingPanel();
        this.add(drawingPanel);
        drawingPanel.setBackground(new Color(255, 255, 255));
        drawingPanel.setBounds(40, 90, 470, 265);

        for (int i = 0; i < drawIcon.length; i++) {
            int index = i;
            JLabel label = new JLabel(new ImageIcon(drawIcon[i]));
            label.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    switch (index) {
                        case 0:
                            currentColor = new Color(255, 12, 12);
                            break;
                        case 1:
                            currentColor = new Color(248, 89, 0);
                            break;
                        case 2:
                            currentColor = new Color(255, 213, 64);
                            break;
                        case 3:
                            currentColor = new Color(23, 189, 9);
                            break;
                        case 4:
                            currentColor = new Color(58, 41, 255);
                            break;
                        case 5:
                            currentColor = new Color(162, 10, 255);
                            break;
                        case 6:
                            currentColor = new Color(255, 60, 212);
                            break;
                        case 7:
                            currentColor = new Color(0, 0, 0);
                            break;
                        case 8:
                            currentColor = new Color(255, 255, 255);
                            break;
                        case 9:
                            drawingPanel.clearDrawing();
                            vector.clear();
                            break;
                    }
                }
            });
            pancelP.add(label);
        }

        JButton exitBtn = new JButton("나가기");
        exitBtn.addActionListener(e -> {
            int result = JOptionPane.showConfirmDialog(null, "정말 게임을 종료하시겠습니까?");
            if (result == JOptionPane.YES_OPTION) {
                this.setVisible(false);
                new Main();
            }
        });

        exitBtn.setBounds(616, 411, 90, 30);
        exitBtn.setBackground(new Color(255, 228, 131));
        exitBtn.setForeground(new Color(142, 110, 0));
        exitBtn.setFont(semiMidFont);
        backgroundPanel.add(exitBtn);

        //제시어 Label 추가 코드
        JLabel categoryJL = new JLabel("제시어");
        categoryJL.setForeground(new Color(89, 89, 89));
        categoryJL.setBounds(540, 358, 200, 50);
        categoryJL.setFont(semiMidFont);
        backgroundPanel.add(categoryJL);

        this.currentTopic = setCurrentTopic(this.currentTopic);
        categoryContentJL = new JLabel(this.currentTopic);
        saveCurrentTopic(this.currentTopic);
        categoryContentJL.setHorizontalAlignment(JLabel.CENTER);
        categoryContentJL.setForeground(new Color(0, 0, 0));
        categoryContentJL.setBounds(513, 392, 100, 50);
        categoryContentJL.setFont(semiMidFont);
        backgroundPanel.add(categoryContentJL);

        // 문제 바꾸는 코드
        JButton changeBtn = new JButton("문제변경");
        changeBtn.addActionListener(e -> {
            this.currentTopic = setCurrentTopic(this.currentTopic);
            categoryContentJL.setText(this.currentTopic);
        });
        changeBtn.setBounds(616, 368, 90, 30);
        changeBtn.setBackground(new Color(255, 228, 131));
        changeBtn.setForeground(new Color(142, 110, 0));
        changeBtn.setFont(smallFont);
        backgroundPanel.add(changeBtn);

        int nameX = 522;
        int nameY = 68;
        //유저 이름 추가
        for (int i = 0; i < nameArr.size(); i++) {
            JLabel nameJL = new JLabel(nameArr.get(i));
            nameJL.setBounds(nameX, nameY, 60, 60);
            backgroundPanel.add(nameJL);
            nameX += 45;

            if (i == 1) {
                nameX = 522;
                nameY = 88;
            }
        }

        add(backgroundPanel);

        JPanel chattingPn = new JPanel(new BorderLayout());
        chattingPn.setBounds(525, 140, 180, 210);

        // 채팅 기록
        chatArea.setEditable(false);
        // 자동 줄바꿈
        chatArea.setLineWrap(true);
        JScrollPane scrollPane = new JScrollPane(chatArea, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        chattingPn.add(scrollPane, BorderLayout.CENTER);

        messageField = new JTextField();
        chattingPn.add(messageField, BorderLayout.SOUTH);

        messageField.addActionListener(e -> {
            sendMessage();
        });

        // 최근 채팅에 포커싱
        focusRecentChat(scrollPane);

        backgroundPanel.add(chattingPn);
        this.setVisible(true);

        connectToServer();
    }

    private static void connectToServer() {
        try {
            Socket socket = new Socket("localhost", 8090);
            writer = new PrintWriter(socket.getOutputStream());
            writer.println(userName);
            writer.flush();
            userCnt++;
            System.out.println("유저수: " + userCnt);

            Thread readerThread = new Thread(new IncomingReader(socket));
            readerThread.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendMessage() {
        String message = messageField.getText();
        writer.println(message);
        if (message.replaceAll(" ", "").contains(currentTopic)) {
            changeCurrentTopic();// 현재 주제 변경
        }
        writer.flush();
        messageField.setText("");

    }

    private void focusRecentChat(JScrollPane scrollPane) {
        scrollPane.getVerticalScrollBar().addAdjustmentListener(new AdjustmentListener() {
            public void adjustmentValueChanged(AdjustmentEvent e) {
                if (e.getAdjustable().getMaximum() != prevMax) {
                    e.getAdjustable().setValue(e.getAdjustable().getMaximum());
                    prevMax = e.getAdjustable().getMaximum(); // 이전 최대 값을 업데이트
                }
            }
        });
    }

    // db table current_topic에 있는 모든 주제 초기화 (GamePlay가 시작될 때만 사용)
    private void dropCurrentTopic() {
        try {
            conn = getConnection(DB.MySQL.JDBC_URL);
            stmt = conn.createStatement();

            String sql = "DELETE FROM current_topic";
            ps = conn.prepareStatement(sql);

            int deleteCount = ps.executeUpdate();
            System.out.println(deleteCount + " 삭제됨");
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("현재 주제 초기화 실패");
        }
    }

    // 현재 주제 정하기
    private static String setCurrentTopic(String currentTopic) {
        try {
            conn = getConnection(DB.MySQL.JDBC_URL);
            stmt = conn.createStatement();

            // 현재 주제 정하기
            rs = stmt.executeQuery("SELECT COUNT(*) FROM topic");

            // 현재 topic table의 사이즈 구하기
            int rowCount = 0;
            while (rs.next()) {
                rowCount = Integer.parseInt(rs.getString("count(*)"));
            }

            // 난수 생성
            double random = Math.random();
            int randomValue = (int) (random * rowCount + 1);

            // 랜덤 주제 가져오기
            rs = stmt.executeQuery("SELECT * FROM topic WHERE id = " + randomValue);
            while (rs.next())
                currentTopic = rs.getString("name");
            System.out.println(currentTopic);

            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("주제 불러오기 실패");
        }

        return currentTopic;
    }

    // 정한 주제 db에 저장
    private static void saveCurrentTopic(String currentTopic) {
        try {
            conn = getConnection(DB.MySQL.JDBC_URL);
            stmt = conn.createStatement();

            String query = "INSERT INTO current_topic (name) VALUES ('" + currentTopic + "')";
            stmt.executeUpdate(query);

            // 사용 후 close
            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("현재 주제 db에 저장 실패");
        }
    }

    // 맞추거나 패스했을 경우, 새 주제 정하기
    private void changeCurrentTopic() {
        writer.println("[ 정답: " + currentTopic + " ]");
        this.currentTopic = setCurrentTopic(this.currentTopic);
        categoryContentJL.setText(this.currentTopic);
        saveCurrentTopic(this.currentTopic);
    }

    static class IncomingReader implements Runnable {
        private Socket socket;
        private Scanner scanner;

        public IncomingReader(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                scanner = new Scanner(socket.getInputStream());
                while (scanner.hasNextLine()) {
                    String message = scanner.nextLine();
                    chatArea.append(message + "\n");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // 타이머
    private void setTimer(JPanel backgroundPanel) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER));

        JLabel c = new JLabel(" : ");
        minute = new JLabel("00");
        second = new JLabel("00");
        new TimerRuning();

        p.add(minute);
        p.add(c);
        p.add(second);

        p.setBounds(615, 90, 90, 40);
        p.setBackground(new Color(255, 255,253));
        backgroundPanel.add(p);

        minute.setFont(new Font("courier", Font.BOLD, 20));
        second.setFont(new Font("courier", Font.BOLD, 20));
        c.setFont(new Font("courier", Font.BOLD, 20));

        minute.setForeground(mainColor);
        second.setForeground(mainColor);
        c.setForeground(mainColor);

    }

    // 타이머 구현
    class TimerRuning {

        public TimerRuning() {

            p_display = new Thread(() -> {
                mm = Integer.parseInt(minute.getText());
                ss = Integer.parseInt(second.getText());

                while (p_display == Thread.currentThread()) {

                    mm = t % (1000 * 60) / 100 / 60;
                    ss = t % (1000 * 60) / 100 % 60;
                    ms = t % 100;

                    try {
                        Thread.sleep(10);

                        minute.setText(String.format("%02d", mm));
                        second.setText(String.format("%02d", ss));

                        t++;

                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    currentTime = String.format("%02d : %02d : %02d", mm, ss, ms);
                    //System.out.println(currentTime);
                }
            });
            p_display.start();
        }
    }

    class DrawingPanel extends JPanel {
        public void clearDrawing() {
            Graphics g = getGraphics();
            g.setColor(getBackground());
            g.fillRect(0, 0, getWidth(), getHeight());
        }


        public DrawingPanel() {
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    x1Temp = e.getX();
                    y1Temp = e.getY();
                    vector.add(x1Temp);
                    vector.add(y1Temp);

                }

            });
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseReleased(MouseEvent e) {
                    x1Temp = e.getX();
                    y1Temp = e.getY();

                }

            });
            addMouseMotionListener(new MouseAdapter() {
                @Override
                public void mouseDragged(MouseEvent e) {

                    // Draw a line with the current color
                    Graphics2D g = (Graphics2D) getGraphics();
                    g.setColor(currentColor);
                    g.drawLine(x1Temp, y1Temp, e.getX(), e.getY());
                    g.setStroke(new BasicStroke(20));
                    x1Temp = e.getX();
                    y1Temp = e.getY();
                    vector.add(e.getX());
                    vector.add(e.getY());
                }
            });
        }
    }


    public static void main(String[] args) {
        new GamePlay(userName);
    }
}