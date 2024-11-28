package org.openimmunizationsoftware.cdsi.core.domain.datatypes;

import java.util.List;

public class Stepper<T> {

    private final List<T> list;
    private int position = -1;

    public Stepper(List<T> list) {
        this.list = list;
    }

    public T getCurrent() {
        if (position == -1 || position >= list.size()) {
            return null;
        }
        return list.get(position);
    }

    public void reset() {
        position = -1;
    }

    public void increment() {
        position++;
    }

    public boolean hasCurrent() {
        return position < list.size();
    }

    public List<T> getList() {
        return list;
    }

    public void setList(List<T> list) {
        this.list.clear();
        this.list.addAll(list);
    }

    public void clearList() {
        list.clear();
    }

    public void add(T t) {
        list.add(t);
    }

    public void addAll(List<T> list) {
        this.list.addAll(list);
    }
}
