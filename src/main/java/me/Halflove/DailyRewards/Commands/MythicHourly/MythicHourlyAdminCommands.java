package me.Halflove.DailyRewards.Commands.MythicHourly;

import me.Halflove.DailyRewards.Main.Main;
import me.Halflove.DailyRewards.Managers.MySQLManager;
import me.Halflove.DailyRewards.Managers.MythicHourly.MythicHourlySettingsManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MythicHourlyAdminCommands implements CommandExecutor {
    private Main plugin;

    public static Connection connection;

    public static String host;

    public static String database;

    public static String username;

    public static String password;

    public int port;

    public MythicHourlyAdminCommands(Main plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(final CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("mythichourlyadmin")) {
            if (sender.isOp() || sender.hasPermission("mh.admin")) {
                if (args.length == 0 || args[0].equalsIgnoreCase("help") || args.length > 2
                        || (!args[0].equalsIgnoreCase("reset") && !args[0].equalsIgnoreCase("reload"))) {
                    sender.sendMessage(ChatColor.BOLD + "DailyRewards Admin Help");
                    sender.sendMessage(ChatColor.YELLOW + "/mh reload" + ChatColor.WHITE + ChatColor.ITALIC
                            + " Reload all DR files.");
                    sender.sendMessage(ChatColor.YELLOW + "/mh reset" + ChatColor.WHITE + ChatColor.ITALIC
                            + " Reset your cooldown.");
                    sender.sendMessage(ChatColor.YELLOW + "/mh reset (player)" + ChatColor.WHITE + ChatColor.ITALIC
                            + " Reset a player's cooldown.");
                    return true;
                }
                if (args[0].equalsIgnoreCase("reload")) {
                    final boolean startmysql;
                    if (!MythicHourlySettingsManager.getConfig().getBoolean("mysql.enabled")) {
                        startmysql = true;
                    } else {
                        startmysql = false;
                    }
                    this.plugin.setting.reloadData();
                    this.plugin.setting.reloadConfig();
                    this.plugin.setting.reloadMsg();
                    MythicHourlySettingsManager.saveData();
                    this.plugin.setting.saveConfig();
                    this.plugin.setting.saveMsg();
                    sender.sendMessage(ChatColor.YELLOW + "DailyRewards is reloading...");
                    (new BukkitRunnable() {
                        public void run() {
                            if (MythicHourlySettingsManager.getConfig().getBoolean("mysql.enabled"))
                                if (startmysql) {
                                    MythicHourlyAdminCommands.this.mysqlSetup();
                                    MySQLManager.createTable();
                                } else {
                                    MySQLManager.createTable();
                                }
                            sender.sendMessage(ChatColor.GREEN + "DailyRewards has been successfully reloaded.");
                        }
                    }).runTaskLater((Plugin) this.plugin, 20L);
                }
                if (args[0].equalsIgnoreCase("reset")) {
                    if (sender instanceof Player) {
                        if (args.length == 1) {
                            Player player = (Player) sender;
                            String ip = player.getAddress().getAddress().getHostAddress();
                            ip = ip.replace(".", "-");
                            MythicHourlySettingsManager.getData().set(String.valueOf(ip) + ".millis", Integer.valueOf(0));
                            MythicHourlySettingsManager.getData().set(player.getUniqueId() + ".millis", Integer.valueOf(0));
                            if (MythicHourlySettingsManager.getConfig().getBoolean("mysql.enabled")) {
                                MySQLManager.updateCooldownIP(ip, 0L);
                                MySQLManager.updateCooldownUUID(player.getUniqueId(), 0L);
                            }
                            sender.sendMessage(ChatColor.GREEN + "You reset your cooldown.");
                        }
                    } else {
                        sender.sendMessage(ChatColor.RED + "Oops, you can't do this in console");
                        sender.sendMessage(ChatColor.RED + "Try '/dr reset (player)' instead");
                    }
                    if (args.length == 2) {
                        Player target = Bukkit.getServer().getPlayer(args[1]);
                        if (target == null) {
                            sender.sendMessage(ChatColor.RED + "The specified player is offline.");
                            return true;
                        }
                        String ip = target.getAddress().getAddress().getHostAddress();
                        ip = ip.replace(".", "-");
                        MythicHourlySettingsManager.getData().set(String.valueOf(ip) + ".millis", Integer.valueOf(0));
                        MythicHourlySettingsManager.getData().set(target.getUniqueId() + ".millis", Integer.valueOf(0));
                        sender.sendMessage(ChatColor.GREEN + "You reset " + target.getName() + "'s cooldown.");
                    }
                }
                return true;
            }
            String msg = MythicHourlySettingsManager.getMsg().getString("no-permission");
            msg = msg.replace("%player", sender.getName());
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
        }
        return true;
    }

    public void mysqlSetup() {
        host = MythicHourlySettingsManager.getConfig().getString("mysql.host-name");
        this.port = MythicHourlySettingsManager.getConfig().getInt("mysql.port");
        database = MythicHourlySettingsManager.getConfig().getString("mysql.database");
        username = MythicHourlySettingsManager.getConfig().getString("mysql.username");
        password = MythicHourlySettingsManager.getConfig().getString("mysql.password");
        try {
            synchronized (this) {
                if (getConnection() != null && !getConnection().isClosed())
                    return;
                Class.forName("com.mysql.jdbc.Driver");
                setConnection(DriverManager.getConnection("jdbc:mysql://" + host + ":" + this.port + "/" + database,
                        username, password));
                Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN + "Daily Rewards MySQL: Successfully Connected");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "Daily Rewards MySQL: Failed To Connected");
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "Daily Rewards MySQL: Error 'SQLException'");
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED
                    + "Daily Rewards MySQL: Your MySQL Configuration Information Is Invalid, Contact Halflove For Support");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "Daily Rewards MySQL: Failed To Connected");
            Bukkit.getConsoleSender()
                    .sendMessage(ChatColor.RED + "Daily Rewards MySQL: Error 'ClassNotFoundException'");
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "Daily Rewards MySQL: Contact Halflove For Support");
        }
    }

    public Connection getConnection() {
        return connection;
    }

    public void setConnection(Connection connection) {
        MythicHourlyAdminCommands.connection = connection;
    }
}
