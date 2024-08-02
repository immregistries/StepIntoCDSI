package org.openimmunizationsoftware.cdsi.core.data;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Iterator;


import org.openimmunizationsoftware.cdsi.core.logic.items.LogicResult;
import org.openimmunizationsoftware.cdsi.core.domain.datatypes.DoseCondition;


import java.util.Arrays;

public enum ConditionPairResult implements Iterable<ConditionPairResult.ConditionResultValuePairs<?>> {

    Table6_3 {
        @Override
        protected void initializePairs() {
            Date dateAdmin = new Date();
            Date lotExpGood = new Date(dateAdmin.getTime() - 1000);
            Date lotExpBad = new Date(dateAdmin.getTime() + 1000);

            conditionPairs = Arrays.asList(
                new ConditionResultValuePairs<Date>(
                    Arrays.asList(dateAdmin, dateAdmin, dateAdmin),
                    Arrays.asList(lotExpGood, lotExpBad, dateAdmin),
                    Arrays.asList(LogicResult.NO, LogicResult.YES, LogicResult.NO)
                ),
                new ConditionResultValuePairs<DoseCondition>(
                    Arrays.asList(DoseCondition.YES, DoseCondition.NO),
                    Arrays.asList(LogicResult.YES, LogicResult.NO)
                )
            );
        }
    };

    List<ConditionResultValuePairs<?>> conditionPairs;
    protected abstract void initializePairs();

   
    private ConditionPairResult() {
        initializePairs();
    }

    @Override
    public Iterator<ConditionResultValuePairs<?>> iterator() {
        return conditionPairs.iterator();
    }

    public class ConditionResultValuePairs<T> implements Iterable<List<Object>> {
        private List<T> first;
        private List<T> second;
        private List<LogicResult> result;

        public ConditionResultValuePairs(List<T> first, List<T> second, List<LogicResult> result) {
            this.first = first;
            this.second = second;
            this.result = result;
        }

        @Override
        public Iterator<List<Object>> iterator() {
            return new CustomIterator();
        }

        private class CustomIterator implements Iterator<List<Object>> {
            private int index = 0;

            @Override
            public boolean hasNext() {
                return index < first.size() && index < second.size() && index < result.size();
            }

            @Override
            public List<Object> next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                List<Object> nextElement = Arrays.asList(first.get(index), second.get(index), result.get(index));
                index++;
                return nextElement;
            }
        }
    }
}
