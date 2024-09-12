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

public class BookService {

    // 查询并更新图书信息
    public static void queryAndUpdateBooks() {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            // 获取数据库连接
            connection = DBUtil.getConnection();
            if (connection != null) {
                // 查询图书信息的SQL语句
                String sql = "SELECT * FROM book";
                preparedStatement = connection.prepareStatement(sql);
                resultSet = preparedStatement.executeQuery();

                // 创建表格模型
                DefaultTableModel model = new DefaultTableModel() {
                    @Override
                    public boolean isCellEditable(int row, int column) {
                        return true; // 所有单元格均可编辑
                    }
                };

                // 添加表格列
                model.addColumn("书号");
                model.addColumn("类别");
                model.addColumn("书名");
                model.addColumn("出版社");
                model.addColumn("作者");
                model.addColumn("价格");
                model.addColumn("总藏书量");
                model.addColumn("库存");

                // 存储初始数据
                Map<String, Object[]> originalData = new HashMap<>();

                // 遍历结果集，将数据添加到表格模型中
                while (resultSet.next()) {
                    String bookNumber = resultSet.getString("book_number");
                    Object[] rowData = new Object[]{
                            bookNumber,
                            resultSet.getString("category"),
                            resultSet.getString("book_name"),
                            resultSet.getString("publisher"),
                            resultSet.getString("author"),
                            resultSet.getDouble("price"),
                            resultSet.getInt("book_total"),
                            resultSet.getInt("inventory")
                    };
                    model.addRow(rowData);
                    originalData.put(bookNumber, rowData);
                }

                // 创建表格并添加到滚动面板中
                JTable table = new JTable(model);
                table.setCellSelectionEnabled(true);  // 允许单元格选择
                JScrollPane scrollPane = new JScrollPane(table);

                // 添加表格模型监听器，用于监听表格数据的修改
                model.addTableModelListener(new TableModelListener() {
                    @Override
                    public void tableChanged(TableModelEvent e) {
                        if (e.getType() == TableModelEvent.UPDATE) {
                            int row = e.getFirstRow();
                            int column = e.getColumn();
                            DefaultTableModel model = (DefaultTableModel) e.getSource();

                            // 获取表格中的数据
                            String oldBookNumber = (String) model.getValueAt(row, 0);
                            String newBookNumber = (String) model.getValueAt(row, 0);
                            String category = (String) model.getValueAt(row, 1);
                            String bookName = (String) model.getValueAt(row, 2);
                            String publisher = (String) model.getValueAt(row, 3);
                            String author = (String) model.getValueAt(row, 4);
                            double price;
                            int bookTotal;
                            int inventory;

                            try {
                                // 解析价格和数量
                                price = Double.parseDouble(model.getValueAt(row, 5).toString());
                                bookTotal = Integer.parseInt(model.getValueAt(row, 6).toString());
                                inventory = Integer.parseInt(model.getValueAt(row, 7).toString());
                            } catch (NumberFormatException ex) {
                                // 如果解析失败，显示错误消息并恢复原始数据
                                JOptionPane.showMessageDialog(null, "价格和数量必须是数字！", "错误", JOptionPane.ERROR_MESSAGE);
                                restoreOriginalData(model, row, column, originalData.get(oldBookNumber));
                                return;
                            }

                            Object[] originalRowData = originalData.get(oldBookNumber);

                            // 更新图书信息，如果失败，恢复原始数据
                            if (!updateBook(oldBookNumber, newBookNumber, category, bookName, publisher, author, price, bookTotal, inventory)) {
                                restoreOriginalData(model, row, column, originalRowData);
                            } else {
                                // 更新成功后，更新原始数据
                                originalData.remove(oldBookNumber);
                                originalData.put(newBookNumber, new Object[]{newBookNumber, category, bookName, publisher, author, price, bookTotal, inventory});
                            }
                        }
                    }

                    // 恢复原始数据的方法
                    private void restoreOriginalData(DefaultTableModel model, int row, int column, Object[] originalRowData) {
                        if (originalRowData != null) {
                            model.setValueAt(originalRowData[column], row, column);
                        }
                    }
                });

                // 创建按钮面板和按钮
                JPanel buttonPanel = new JPanel();
                JButton addButton = new JButton("添加图书");
                JButton deleteButton = new JButton("删除图书");

                // 添加图书按钮的事件监听器
                addButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        // 创建文本字段用于输入图书信息
                        JTextField bookNumberField = new JTextField();
                        JTextField categoryField = new JTextField();
                        JTextField bookNameField = new JTextField();
                        JTextField publisherField = new JTextField();
                        JTextField authorField = new JTextField();
                        JTextField priceField = new JTextField();
                        JTextField bookTotalField = new JTextField();
                        JTextField inventoryField = new JTextField();

                        // 创建对话框用于输入图书信息
                        Object[] message = {
                                "书号:", bookNumberField,
                                "类别:", categoryField,
                                "书名:", bookNameField,
                                "出版社:", publisherField,
                                "作者:", authorField,
                                "价格:", priceField,
                                "总藏书量:", bookTotalField,
                                "库存:", inventoryField
                        };

                        // 显示对话框并获取用户输入
                        int option = JOptionPane.showConfirmDialog(null, message, "添加图书", JOptionPane.OK_CANCEL_OPTION);
                        if (option == JOptionPane.OK_OPTION) {
                            try {
                                // 解析用户输入的数据
                                String bookNumber = bookNumberField.getText();
                                String category = categoryField.getText();
                                String bookName = bookNameField.getText();
                                String publisher = publisherField.getText();
                                String author = authorField.getText();
                                double price = Double.parseDouble(priceField.getText());
                                int bookTotal = Integer.parseInt(bookTotalField.getText());
                                int inventory = Integer.parseInt(inventoryField.getText());

                                // 添加图书信息
                                addOrDeleteBook(model, "add", bookNumber, category, bookName, publisher, author, price, bookTotal, inventory);
                            } catch (NumberFormatException ex) {
                                JOptionPane.showMessageDialog(null, "价格和数量必须是数字！", "错误", JOptionPane.ERROR_MESSAGE);
                            }
                        }
                    }
                });

                // 删除图书按钮的事件监听器
                deleteButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        // 创建文本字段用于输入书号
                        JTextField bookNumberField = new JTextField();
                        Object[] message = {
                                "书号:", bookNumberField
                        };

                        // 显示对话框并获取用户输入
                        int option = JOptionPane.showConfirmDialog(null, message, "删除图书", JOptionPane.OK_CANCEL_OPTION);
                        if (option == JOptionPane.OK_OPTION) {
                            String bookNumber = bookNumberField.getText();
                            // 删除图书信息
                            addOrDeleteBook(model, "delete", bookNumber, null, null, null, null, 0, 0, 0);
                        }
                    }
                });

                // 将按钮添加到按钮面板
                buttonPanel.add(addButton);
                buttonPanel.add(deleteButton);

                // 创建窗口并添加组件
                JFrame frame = new JFrame("图书信息");
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
            // 关闭数据库资源
            if (resultSet != null) {
                try {
                    resultSet.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // 添加或删除图书的方法
    public static void addOrDeleteBook(DefaultTableModel model, String action, String bookNumber, String category, String bookName, String publisher, String author, double price, int bookTotal, int inventory) {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        try {
            // 获取数据库连接
            connection = DBUtil.getConnection();
            if (connection != null) {
                String sql;
                if ("add".equals(action)) {
                    // 添加图书的SQL语句
                    sql = "INSERT INTO book (book_number, category, book_name, publisher, author, price, book_total, inventory) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
                    preparedStatement = connection.prepareStatement(sql);
                    preparedStatement.setString(1, bookNumber);
                    preparedStatement.setString(2, category);
                    preparedStatement.setString(3, bookName);
                    preparedStatement.setString(4, publisher);
                    preparedStatement.setString(5, author);
                    preparedStatement.setDouble(6, price);
                    preparedStatement.setInt(7, bookTotal);
                    preparedStatement.setInt(8, inventory);
                    preparedStatement.executeUpdate();
                    // 在表格模型中添加新行
                    model.addRow(new Object[]{bookNumber, category, bookName, publisher, author, price, bookTotal, inventory});
                } else if ("delete".equals(action)) {
                    // 删除图书的SQL语句
                    sql = "DELETE FROM book WHERE book_number = ?";
                    preparedStatement = connection.prepareStatement(sql);
                    preparedStatement.setString(1, bookNumber);
                    int rowsDeleted = preparedStatement.executeUpdate();
                    if (rowsDeleted > 0) {
                        // 从表格模型中删除对应行
                        for (int i = 0; i < model.getRowCount(); i++) {
                            if (model.getValueAt(i, 0).equals(bookNumber)) {
                                model.removeRow(i);
                                break;
                            }
                        }
                    } else {
                        JOptionPane.showMessageDialog(null, "书号不存在！", "错误", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            // 关闭数据库资源
            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // 更新图书信息的方法
    public static boolean updateBook(String oldBookNumber, String newBookNumber, String category, String bookName, String publisher, String author, double price, int bookTotal, int inventory) {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        try {
            // 获取数据库连接
            connection = DBUtil.getConnection();
            if (connection != null) {
                // 更新图书信息的SQL语句
                String sql = "UPDATE book SET book_number = ?, category = ?, book_name = ?, publisher = ?, author = ?, price = ?, book_total = ?, inventory = ? WHERE book_number = ?";
                preparedStatement = connection.prepareStatement(sql);
                preparedStatement.setString(1, newBookNumber);
                preparedStatement.setString(2, category);
                preparedStatement.setString(3, bookName);
                preparedStatement.setString(4, publisher);
                preparedStatement.setString(5, author);
                preparedStatement.setDouble(6, price);
                preparedStatement.setInt(7, bookTotal);
                preparedStatement.setInt(8, inventory);
                preparedStatement.setString(9, oldBookNumber);
                int rowsUpdated = preparedStatement.executeUpdate();
                return rowsUpdated > 0;  // 返回是否更新成功
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            // 关闭数据库资源
            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
        return false;  // 更新失败
    }
}
