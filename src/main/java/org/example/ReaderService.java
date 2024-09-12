package org.example;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class ReaderService {

    // 标记是否正在恢复数据，避免循环触发更新事件
    private static boolean isRestoringData = false;

    // 显示读者管理界面
    public static void showReaderManagementUI() {
        SwingUtilities.invokeLater(() -> queryAndUpdateReaders());
    }

    // 查询并更新读者信息
    public static void queryAndUpdateReaders() {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            connection = DBUtil.getConnection();
            if (connection != null) {
                String sql = "SELECT * FROM reader";
                preparedStatement = connection.prepareStatement(sql);
                resultSet = preparedStatement.executeQuery();

                // 创建表格模型
                DefaultTableModel model = new DefaultTableModel() {
                    @Override
                    public boolean isCellEditable(int row, int column) {
                        return true;
                    }
                };

                // 添加列名
                model.addColumn("读者编号");
                model.addColumn("姓名");
                model.addColumn("单位");
                model.addColumn("性别");
                model.addColumn("电话");

                // 存储原始数据，用于恢复
                Map<String, Object[]> originalData = new HashMap<>();

                while (resultSet.next()) {
                    String readerNumber = resultSet.getString("reader_number");
                    Object[] rowData = new Object[]{
                            readerNumber,
                            resultSet.getString("name"),
                            resultSet.getString("department"),
                            resultSet.getString("gender"),
                            resultSet.getString("telephone")
                    };
                    model.addRow(rowData);
                    originalData.put(readerNumber, rowData.clone());
                }

                // 创建表格
                JTable table = new JTable(model);
                table.setCellSelectionEnabled(true);
                JScrollPane scrollPane = new JScrollPane(table);

                // 添加表格模型监听器
                model.addTableModelListener(new TableModelListener() {
                    @Override
                    public void tableChanged(TableModelEvent e) {
                        if (e.getType() == TableModelEvent.UPDATE && !isRestoringData) {
                            int row = e.getFirstRow();
                            DefaultTableModel model = (DefaultTableModel) e.getSource();

                            // 获取更新后的数据
                            String oldReaderNumber = (String) model.getValueAt(row, 0);
                            String newReaderNumber = (String) model.getValueAt(row, 0);
                            String name = (String) model.getValueAt(row, 1);
                            String department = (String) model.getValueAt(row, 2);
                            String gender = (String) model.getValueAt(row, 3);
                            String telephone = (String) model.getValueAt(row, 4);

                            // 获取原始数据
                            Object[] originalRowData = originalData.get(oldReaderNumber);

                            // 数据校验
                            if (!DataValidation.isValidGender(gender) || !DataValidation.isValidTelephone(telephone)) {
                                JOptionPane.showMessageDialog(null, "性别必须为“男”或“女”，电话必须符合格式要求！", "错误", JOptionPane.ERROR_MESSAGE);
                                isRestoringData = true;
                                restoreOriginalData(model, row, originalRowData);
                                isRestoringData = false;
                                return;
                            }

                            // 更新数据库
                            if (!updateReader(oldReaderNumber, newReaderNumber, name, department, gender, telephone)) {
                                JOptionPane.showMessageDialog(null, "只可更新为已有的读者编号！", "错误", JOptionPane.ERROR_MESSAGE);
                                isRestoringData = true;
                                restoreOriginalData(model, row, originalRowData);
                                isRestoringData = false;
                            } else {
                                if (!oldReaderNumber.equals(newReaderNumber)) {
                                    originalData.remove(oldReaderNumber);
                                }
                                originalData.put(newReaderNumber, new Object[]{newReaderNumber, name, department, gender, telephone});
                            }
                        }
                    }

                    // 恢复原始数据
                    private void restoreOriginalData(DefaultTableModel model, int row, Object[] originalRowData) {
                        if (originalRowData != null) {
                            for (int i = 0; i < originalRowData.length; i++) {
                                model.setValueAt(originalRowData[i], row, i);
                            }
                        }
                    }
                });

                // 按钮面板
                JPanel buttonPanel = new JPanel();
                JButton addButton = new JButton("添加读者");
                JButton deleteButton = new JButton("删除读者");
                JButton queryBorrowingButton = new JButton("查询借书信息");
                JButton queryReturningButton = new JButton("查询还书信息");

                // 添加读者按钮事件
                addButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        // 弹出输入对话框
                        JTextField readerNumberField = new JTextField();
                        JTextField nameField = new JTextField();
                        JTextField departmentField = new JTextField();
                        JTextField genderField = new JTextField();
                        JTextField telephoneField = new JTextField();

                        Object[] message = {
                                "读者编号:", readerNumberField,
                                "姓名:", nameField,
                                "单位:", departmentField,
                                "性别:", genderField,
                                "电话:", telephoneField
                        };

                        int option = JOptionPane.showConfirmDialog(null, message, "添加读者", JOptionPane.OK_CANCEL_OPTION);
                        if (option == JOptionPane.OK_OPTION) {
                            try {
                                String readerNumber = readerNumberField.getText();
                                String name = nameField.getText();
                                String department = departmentField.getText();
                                String gender = genderField.getText();
                                String telephone = telephoneField.getText();

                                if (!DataValidation.isValidGender(gender) || !DataValidation.isValidTelephone(telephone)) {
                                    JOptionPane.showMessageDialog(null, "性别必须为“男”或“女”，电话必须符合格式要求！", "错误", JOptionPane.ERROR_MESSAGE);
                                    return;
                                }

                                // 调用添加或删除读者方法
                                addOrDeleteReader(model, "add", readerNumber, name, department, gender, telephone);
                            } catch (Exception ex) {
                                JOptionPane.showMessageDialog(null, "输入信息有误，请检查！", "错误", JOptionPane.ERROR_MESSAGE);
                            }
                        }
                    }
                });

                // 删除读者按钮事件
                deleteButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        JTextField bookNumberField = new JTextField();
                        Object[] message = {
                                "请输入要删除的读者编号:", bookNumberField
                        };

                        int option = JOptionPane.showConfirmDialog(null, message, "删除图书", JOptionPane.OK_CANCEL_OPTION);
                        if (option == JOptionPane.OK_OPTION) {
                            String inputBookNumber = bookNumberField.getText().trim();
                            if (!inputBookNumber.equals("")) {
                                int confirm = JOptionPane.showConfirmDialog(null, "确定要删除编号为 " + inputBookNumber + " 的读者吗？", "删除确认", JOptionPane.YES_NO_OPTION);
                                if (confirm == JOptionPane.YES_OPTION) {
                                    // 调用添加或删除
                                    addOrDeleteReader(model, "delete", inputBookNumber, null, null, null, null);
                                }
                            } else {
                                JOptionPane.showMessageDialog(null, "请输入有效的读者编号！", "错误", JOptionPane.ERROR_MESSAGE);
                            }
                        }
                    }
                });
                // 查询借书信息按钮事件
                queryBorrowingButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        String readerNumber = JOptionPane.showInputDialog(null, "请输入读者编号:", "查询借书信息", JOptionPane.PLAIN_MESSAGE);
                        if (readerNumber != null && !readerNumber.trim().isEmpty()) {
                            queryReaderBorrowingInfo(readerNumber.trim());
                        } else {
                            JOptionPane.showMessageDialog(null, "读者编号不能为空！", "错误", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                });

                // 查询还书信息按钮事件
                queryReturningButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        String readerNumber = JOptionPane.showInputDialog(null, "请输入读者编号:", "查询还书信息", JOptionPane.PLAIN_MESSAGE);
                        if (readerNumber != null && !readerNumber.trim().isEmpty()) {
                            queryReaderReturningInfo(readerNumber.trim());
                        } else {
                            JOptionPane.showMessageDialog(null, "读者编号不能为空！", "错误", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                });

                // 添加按钮到按钮面板
                buttonPanel.add(addButton);
                buttonPanel.add(deleteButton);
                buttonPanel.add(queryBorrowingButton);
                buttonPanel.add(queryReturningButton);

                // 创建窗口并添加组件
                JFrame frame = new JFrame("读者信息");
                frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                frame.add(scrollPane, BorderLayout.CENTER);
                frame.add(buttonPanel, BorderLayout.SOUTH);
                frame.setSize(800, 600);
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            closeResources(connection, preparedStatement, resultSet);
        }
    }

    // 添加或删除读者
    public static void addOrDeleteReader(DefaultTableModel model, String action, String readerNumber, String name, String department, String gender, String telephone) {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        try {
            connection = DBUtil.getConnection();
            if (connection != null) {
                if ("add".equalsIgnoreCase(action)) {
                    String sql = "INSERT INTO reader (reader_number, name, department, gender, telephone) VALUES (?, ?, ?, ?, ?)";
                    preparedStatement = connection.prepareStatement(sql);
                    preparedStatement.setString(1, readerNumber);
                    preparedStatement.setString(2, name);
                    preparedStatement.setString(3, department);
                    preparedStatement.setString(4, gender);
                    preparedStatement.setString(5, telephone);
                    int rowsAffected = preparedStatement.executeUpdate();
                    if (rowsAffected > 0) {
                        model.addRow(new Object[]{readerNumber, name, department, gender, telephone});
                        JOptionPane.showMessageDialog(null, "读者添加成功！");
                    } else {
                        JOptionPane.showMessageDialog(null, "添加读者失败，请重试！", "错误", JOptionPane.ERROR_MESSAGE);
                    }
                } else if ("delete".equalsIgnoreCase(action)) {
                    String sql = "DELETE FROM reader WHERE reader_number = ?";
                    preparedStatement = connection.prepareStatement(sql);
                    preparedStatement.setString(1, readerNumber);
                    int rowsAffected = preparedStatement.executeUpdate();
                    if (rowsAffected > 0) {
                        for (int i = 0; i < model.getRowCount(); i++) {
                            if (model.getValueAt(i, 0).equals(readerNumber)) {
                                model.removeRow(i);
                                break;
                            }
                        }
                        JOptionPane.showMessageDialog(null, "读者删除成功！");
                    } else {
                        JOptionPane.showMessageDialog(null, "删除读者失败，请重试！", "错误", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            closeResources(connection, preparedStatement, null);
        }
    }

    // 更新读者信息
    public static boolean updateReader(String oldReaderNumber, String newReaderNumber, String name, String department, String gender, String telephone) {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        try {
            connection = DBUtil.getConnection();
            if (connection != null) {
                String sql = "UPDATE reader SET reader_number = ?, name = ?, department = ?, gender = ?, telephone = ? WHERE reader_number = ?";
                preparedStatement = connection.prepareStatement(sql);
                preparedStatement.setString(1, newReaderNumber);
                preparedStatement.setString(2, name);
                preparedStatement.setString(3, department);
                preparedStatement.setString(4, gender);
                preparedStatement.setString(5, telephone);
                preparedStatement.setString(6, oldReaderNumber);
                int rowsAffected = preparedStatement.executeUpdate();
                return rowsAffected > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            closeResources(connection, preparedStatement, null);
        }
        return false;
    }

    // 查询读者借书信息
    public static void queryReaderBorrowingInfo(String readerNumber) {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            connection = DBUtil.getConnection();
            if (connection != null) {
                String sql = "SELECT bb.reader_number, bb.book_number, b.book_name, b.publisher, bb.borrow_time " +
                        "FROM borrow_book bb " +
                        "JOIN book b ON bb.book_number = b.book_number " +
                        "WHERE bb.reader_number = ?";
                preparedStatement = connection.prepareStatement(sql);
                preparedStatement.setString(1, readerNumber);
                resultSet = preparedStatement.executeQuery();

                DefaultTableModel model = new DefaultTableModel();
                model.addColumn("读者编号");
                model.addColumn("书号");
                model.addColumn("书名");
                model.addColumn("出版社");
                model.addColumn("借书时间");

                while (resultSet.next()) {
                    model.addRow(new Object[]{
                            resultSet.getString("reader_number"),
                            resultSet.getString("book_number"),
                            resultSet.getString("book_name"),
                            resultSet.getString("publisher"),
                            resultSet.getTimestamp("borrow_time")
                    });
                }

                JTable table = new JTable(model);
                table.setEnabled(false);
                JScrollPane scrollPane = new JScrollPane(table);

                JFrame frame = new JFrame("借书信息");
                frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                frame.add(scrollPane, BorderLayout.CENTER);
                frame.setSize(800, 600);
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            closeResources(connection, preparedStatement, resultSet);
        }
    }

    // 查询读者还书信息
    public static void queryReaderReturningInfo(String readerNumber) {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            connection = DBUtil.getConnection();
            if (connection != null) {
                String sql = "SELECT rb.reader_number, rb.book_number, b.book_name, b.publisher, rb.return_time " +
                        "FROM return_book rb " +
                        "JOIN book b ON rb.book_number = b.book_number " +
                        "WHERE rb.reader_number = ?";
                preparedStatement = connection.prepareStatement(sql);
                preparedStatement.setString(1, readerNumber);
                resultSet = preparedStatement.executeQuery();

                DefaultTableModel model = new DefaultTableModel();
                model.addColumn("读者编号");
                model.addColumn("书号");
                model.addColumn("书名");
                model.addColumn("出版社");
                model.addColumn("还书时间");
                while (resultSet.next()) {
                    model.addRow(new Object[]{
                            resultSet.getString("reader_number"),
                            resultSet.getString("book_number"),
                            resultSet.getString("book_name"),
                            resultSet.getString("publisher"),
                            resultSet.getTimestamp("return_time")
                    });
                }

                JTable table = new JTable(model);
                table.setEnabled(false);
                JScrollPane scrollPane = new JScrollPane(table);

                JFrame frame = new JFrame("还书信息");
                frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                frame.add(scrollPane, BorderLayout.CENTER);
                frame.setSize(800, 600);
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            closeResources(connection, preparedStatement, resultSet);
        }
    }

    // 关闭数据库资源
    public static void closeResources(Connection connection, PreparedStatement preparedStatement, ResultSet resultSet) {
        try {
            if (resultSet != null) {
                resultSet.close();
            }
            if (preparedStatement != null) {
                preparedStatement.close();
            }
            if (connection != null) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
