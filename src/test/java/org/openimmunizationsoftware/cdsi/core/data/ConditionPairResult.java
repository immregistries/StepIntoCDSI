package org.openimmunizationsoftware.cdsi.core.data;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Iterator;


import org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.DoseCondition;

import java.util.Map;
import java.util.HashMap;

public enum ConditionPairResult implements Iterable<ConditionPairResult.ConditionResult> {
    Table6_3 {
        @Override
        protected void initializePairs() {
            Date dateAdmin = new Date();
            Date lotExpBad = new Date(dateAdmin.getTime() - 1000);
            Date lotExpGood = new Date(dateAdmin.getTime() + 1000);

            conditionPairs = new HashMap<>();
            conditionPairs.put("Date", Arrays.asList(
                new ConditionResultValuePairs<>(
                    Arrays.asList(dateAdmin, dateAdmin, dateAdmin),
                    Arrays.asList(lotExpBad, lotExpGood, dateAdmin),
                    Arrays.asList(LogicResult.YES, LogicResult.NO, LogicResult.NO)
                )
            ));
            conditionPairs.put("DoseCondition", Arrays.asList(
                new ConditionResultValue<>(
                    Arrays.asList(DoseCondition.YES, DoseCondition.NO),
                    Arrays.asList(LogicResult.YES, LogicResult.NO) 
                )
            ));
        }
    };

    // Updated to use the common interface
    protected Map<String, List<ConditionResult>> conditionPairs;
    protected abstract void initializePairs();

    private ConditionPairResult() {
        initializePairs();
    }

    @Override
    public Iterator<ConditionResult> iterator() {
        return new ConditionPairResultIterator();
    }

    private class ConditionPairResultIterator implements Iterator<ConditionResult> {
        private final Iterator<List<ConditionResult>> mapIterator;
        private Iterator<ConditionResult> listIterator;

        public ConditionPairResultIterator() {
            mapIterator = conditionPairs.values().iterator();
            if (mapIterator.hasNext()) {
                listIterator = mapIterator.next().iterator();
            }
        }

        @Override
        public boolean hasNext() {
            return (listIterator != null && listIterator.hasNext()) || (mapIterator.hasNext());
        }

        @Override
        public ConditionResult next() {
            if (listIterator != null && listIterator.hasNext()) {
                return listIterator.next();
            } else if (mapIterator.hasNext()) {
                listIterator = mapIterator.next().iterator();
                return listIterator.next();
            }
            throw new NoSuchElementException();
        }
    }

    public interface ConditionResult {
        abstract List<Object> get(int index);
        abstract int size();
    }

    public static class ConditionResultValue<T> implements ConditionResult {
        private List<T> first;
        private List<LogicResult> result;

        public ConditionResultValue(List<T> first, List<LogicResult> result) {
            this.first = first;
            this.result = result;
        }

        public List<Object> get(int index) {
            return Arrays.asList(first.get(index), result.get(index));
        }

        public int size() {
            return first.size();
        }
    }

    public static class ConditionResultValuePairs<T>  implements ConditionResult {
        private List<T> first;
        private List<T> second;
        private List<LogicResult> result;

        public ConditionResultValuePairs(List<T> first, List<T> second, List<LogicResult> result) {
            this.first = first;
            this.second = second;
            this.result = result;
        }

        public List<Object> get(int index) {
            return Arrays.asList(first.get(index), second.get(index), result.get(index));
        }

        public int size() {
            return first.size();
        }
    }

    public static class ConditionResultValueTriplet<T> implements ConditionResult {
        private List<T> first;
        private List<T> second;
        private List<T> third;
        private List<LogicResult> result;
        
        public ConditionResultValueTriplet(List<T> first, List<T> second, List<T> third, List<LogicResult> result) {
            this.first = first;
            this.second = second;
            this.third = third;
            this.result = result;
        }

        public List<Object> get(int index) {
            return Arrays.asList(first.get(index), second.get(index), third.get(index), result.get(index));
        }

        public int size() {
            return first.size();
        }
    }
}
