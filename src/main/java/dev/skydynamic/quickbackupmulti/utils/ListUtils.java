package dev.skydynamic.quickbackupmulti.utils;

import dev.skydynamic.quickbackupmulti.utils.storage.BackupInfo;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Objects;

import static dev.skydynamic.quickbackupmulti.QuickBackupMulti.LOGGER;
import static dev.skydynamic.quickbackupmulti.i18n.Translate.tr;
import static dev.skydynamic.quickbackupmulti.utils.DataBase.getDatabase;
import static dev.skydynamic.quickbackupmulti.utils.QbmManager.getBackupDir;
import static dev.skydynamic.quickbackupmulti.utils.QbmManager.getBackupsList;

public class ListUtils {
    private static final DataBase dataBase = getDatabase();

    private static long getDirSize(File dir) {
        return FileUtils.sizeOf(dir);
    }

    private static int getPageCount(List<String>backupsDirList, int page) {
        int size = backupsDirList.size();
        if (!(size < 5*page)) {
            return 5;
        } else if (size < 5*page && (size < 5 && size > 0)){
            return size;
        } else {
            return Math.max(size - 5 * (page - 1), 0);
        }
    }

    public static int getTotalPage(List<String> backupsList) {
        return (int) Math.ceil(backupsList.size() / 5.0);
    }

    private static MutableText getBackPageText(int page, int totalPage) {
        MutableText backPageText;
        backPageText = Messenger.literal("[<-]");
        backPageText.styled(style -> style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.of(tr("quickbackupmulti.list_backup.back_page")))));
        if (page != 1 && totalPage > 1) {
            backPageText.styled(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/qb list " + (page - 1))))
                .styled(style -> style.withColor(Formatting.AQUA));
        } else if (page == 1) {
            backPageText.styled(style -> style.withColor(Formatting.DARK_GRAY));
        }
        return backPageText;
    }

    private static MutableText getNextPageText(int page, int totalPage) {
        MutableText nextPageText;
        nextPageText = Messenger.literal("[->]");
        nextPageText.styled(style -> style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.of(tr("quickbackupmulti.list_backup.next_page")))));
        if (page != totalPage && totalPage > 1) {
            nextPageText.styled(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/qb list " + (page + 1))))
                .styled(style -> style.withColor(Formatting.AQUA));
        } else if (page == totalPage) {
            nextPageText.styled(style -> style.withColor(Formatting.DARK_GRAY));
        }
        return nextPageText;
    }

    private static MutableText getSlotText(String name, int page, int num, long backupSizeB) throws IOException {
        MutableText backText = Messenger.literal("§2[▷] ");
        MutableText deleteText = Messenger.literal("§c[×] ");
        MutableText resultText = Messenger.literal("");
        // var reader = new FileReader(backupDir.resolve(name + "_info.json").toFile());
        // var result = gson.fromJson(reader, BackupInfo.class);
        // reader.close();
        BackupInfo result = dataBase.getSlotInfo(name);
        backText.styled(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/qb back " + name)))
            .styled(style -> style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.of(tr("quickbackupmulti.list_backup.slot.restore", name)))));
        deleteText.styled(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/qb delete " + name)))
            .styled(style -> style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.of(tr("quickbackupmulti.list_backup.slot.delete", name)))));
        String desc = result.getDesc();
        if (Objects.equals(result.getDesc(), "")) desc = tr("quickbackupmulti.empty_comment");
        double backupSizeMB = (double) backupSizeB / FileUtils.ONE_MB;
        double backupSizeGB = (double) backupSizeB / FileUtils.ONE_GB;
        String sizeString = (backupSizeMB >= 1000) ? String.format("%.2fGB", backupSizeGB) : String.format("%.2fMB", backupSizeMB);
        resultText.append("\n" + tr("quickbackupmulti.list_backup.slot.header",  num + (5 * (page - 1))) + " ")
            .append("§6" + name + "§r ")
            .append(backText)
            .append(deleteText)
            .append("§a" + sizeString)
            .append(String.format(" §b%s§7: §r%s", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(result.getTimestamp()), desc));
        return resultText;
    }

    public static MutableText list(int page) {
        long totalBackupSizeB = 0;
        Path backupDir = getBackupDir();
        List<String> backupsList = getBackupsList();
        if (backupsList.isEmpty() || getPageCount(backupsList, page) == 0) {
            return Messenger.literal(tr("quickbackupmulti.list_empty"));
        }
        int totalPage = getTotalPage(backupsList);

        MutableText resultText = Messenger.literal(tr("quickbackupmulti.list_backup.title", page));
        MutableText backPageText = getBackPageText(page, totalPage);
        MutableText nextPageText = getNextPageText(page, totalPage);
        resultText.append("\n")
            .append(backPageText)
            .append("  ")
            .append(tr("quickbackupmulti.list_backup.page_msg", page, totalPage))
            .append("  ")
            .append(nextPageText);

        for (int j=1;j<=getPageCount(backupsList, page);j++) {
            try {
                String name = backupsList.get(((j-1)+5*(page-1)));
                long backupSizeB = getDirSize(backupDir.resolve(name).toFile());
                totalBackupSizeB += backupSizeB;
                resultText.append(getSlotText(name, page, j, backupSizeB));
            } catch (IOException e) {
                LOGGER.error("FileNotFoundException: " + e.getMessage());
            }
        }
        double totalBackupSizeMB = (double) totalBackupSizeB / FileUtils.ONE_MB;
        double totalBackupSizeGB = (double) totalBackupSizeB / FileUtils.ONE_GB;
        String sizeString = (totalBackupSizeMB >= 1000) ? String.format("%.2fGB", totalBackupSizeGB) : String.format("%.2fMB", totalBackupSizeMB);
        resultText.append("\n" + tr("quickbackupmulti.list_backup.slot.total_space", sizeString));
        return resultText;
    }

    public static MutableText search(List<String> searchResultList) {
        MutableText resultText = Messenger.literal(tr("quickbackupmulti.search.success"));
        Path backupDir = getBackupDir();
        for (int i=1;i<=searchResultList.size();i++) {
            try {
                String name = searchResultList.get(i-1);
                long backupSizeB = getDirSize(backupDir.resolve(name).toFile());
                resultText.append(getSlotText(name, 1, i, backupSizeB));
            } catch (IOException e) {
                LOGGER.error("FileNotFoundException: " + e.getMessage());
            }
        }
        return resultText;
    }
}
