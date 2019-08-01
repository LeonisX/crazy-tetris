package md.leonis.tetris;

import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.io.IOException;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.BufferedWriter;
import java.io.BufferedReader;
import javax.swing.table.DefaultTableModel;

public class Records {
    private String fileName;
    private int maxRecords = 10;
    private List<Rec> recordsList;
    private Comparator<Rec> comparator;

    public Records(String fileName) {
        this.fileName = fileName;
        recordsList = new ArrayList<>();
        comparator = (o1, o2) -> Integer.compare(o2.record, o1.record);
        restore();
    }

    public void add(String s, int k) {
        recordsList.add(new Rec(s, k));
    }

    public void verifyAndAdd(String s, int k) {
        if (recordsList.size() < maxRecords) recordsList.add(new Rec(s, k));
        else if (k > recordsList.get(recordsList.size() - 1).record) {
            recordsList.remove(recordsList.size() - 1);
            recordsList.add(new Rec(s, k));
        }
        sort();
    }

    public boolean verify(int k) {
        boolean result = true;
        if (recordsList.size() == 0) return result;
        if (recordsList.size() < maxRecords) return result;
//        String s=k+" | "+recordsList.get(recordsList.size()-1).record;
//        System.out.println(s);
        if (k < recordsList.get(recordsList.size() - 1).record) result = false;
        return result;
    }

    public int getPlace(int k) {
        int n = 254;
        if (recordsList.size() == 0) return 1;
        for (int i = recordsList.size() - 1; i >= 0; i--) {
            if (recordsList.get(i).record < k) n = i;
        }
        n++;
        return n;
    }

    private void sort() {
        recordsList.sort(comparator);
    }

    public void list() {
        for (Rec r : recordsList) System.out.println(r.name + ": " + r.record);
    }

    private void restore() {
        recordsList.clear();
        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            int n = 0;
            String s, s2;
            while (true) {
                s = br.readLine();
                if (s == null) break;
                s2 = br.readLine();
                if (s2 == null) break;
                try {
                    n = Integer.parseInt(s2);
                } catch (NumberFormatException nfe) {
//                    nfe.printStackTrace();
                    n = 0;
                }
                recordsList.add(new Rec(s, n));
            }
        } catch (Exception e) {
//            e.printStackTrace();
            //TODO
        }
        //                ex.printStackTrace();
        sort();
    }

    public void save() {
        BufferedWriter bw = null;
        try {
            bw = new BufferedWriter(new FileWriter(fileName));
            for (Rec r : recordsList) {
                bw.write(r.name);
                bw.newLine();
                bw.write(Integer.toString(r.record));
                bw.newLine();
            }
        } catch (Exception e) {
//            e.printStackTrace();
            //TODO
        } finally {
            try {
                if (bw != null) {
                    bw.flush();
                    bw.close();
                }
            } catch (IOException ex) {
//                ex.printStackTrace();
            }
        }
    }

    public void fillModel(DefaultTableModel model) {
        for (int i = model.getRowCount() - 1; i >= 0; i--) model.removeRow(i);
        for (Rec r : recordsList) model.addRow(new Object[]{r.name, r.record});
    }

    static class Rec {
        String name;
        int record;

        Rec(String name, int record) {
            this.name = name;
            this.record = record;
        }
    }
}
