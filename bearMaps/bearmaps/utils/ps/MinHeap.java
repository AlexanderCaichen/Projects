package bearmaps.utils.ps;

import java.util.NoSuchElementException;

public class MinHeap <E extends Comparable<E>> {

    private E[] minHeapArray = (E[]) new Object[3000];
    private int size;


    public MinHeap() {

    }

    public void add(E item) {
        if (size == minHeapArray.length - 1) {
            E[] tempResizeArray = (E[]) new Object[minHeapArray.length * 2];
            for (int i = 1; i < minHeapArray.length; i ++) {
                tempResizeArray[i] = minHeapArray[i];
            }
            minHeapArray = tempResizeArray;
        }

        minHeapArray[size] = item;
        size++;
        bubbleUp(size);

    }

    private void bubbleDown(int index) {

        if (index == size) {
            return;
        }

        boolean leftIsSmallerOrEqual = (minHeapArray[index*2].compareTo(minHeapArray[index]) <= 0);
        boolean rightIsSmaller = (minHeapArray[index*2 +1].compareTo(minHeapArray[index]) < 0);
        boolean leftIsSmallerThanRight = (minHeapArray[index*2].compareTo(minHeapArray[index *2 +1]) < 0);

        if (leftIsSmallerOrEqual && rightIsSmaller) {

                if (leftIsSmallerThanRight) {
                    E temp = minHeapArray[index];
                    minHeapArray[index] =  minHeapArray[index*2];
                    minHeapArray[index*2] = temp;
                    bubbleDown(index*2);
                } else {
                    E temp = minHeapArray[index];
                    minHeapArray[index] =  minHeapArray[index*2 +1];
                    minHeapArray[index*2 +1] = temp;
                    bubbleDown(index*2 +1);
                }
            return;
        }

        if (rightIsSmaller) {
            E temp = minHeapArray[index];
            minHeapArray[index] =  minHeapArray[index*2 +1];
            minHeapArray[index*2 +1] = temp;
            bubbleDown(index*2 +1);
            return;
        }

        if (leftIsSmallerOrEqual) {
            E temp = minHeapArray[index];
            minHeapArray[index] =  minHeapArray[index*2];
            minHeapArray[index*2] = temp;
            bubbleDown(index*2);
            return;
        }
    }

    private void bubbleUp(int index) {


        if (index == 1) {
            return;
        }

        if (minHeapArray[index/2].compareTo(minHeapArray[index]) > 0) {
            E temp = minHeapArray[index];
            minHeapArray[index] =  minHeapArray[index/2];
            minHeapArray[index/2] = temp;
            bubbleUp(index/2);
        }


    }

    public E poll() {

        E toReturn = minHeapArray[1];
        minHeapArray[1] = minHeapArray[size];
        minHeapArray[size] = null;
        size--;
        bubbleDown(1);
        return toReturn;
    }

    public E peak() {
        return minHeapArray[1];
    }

    public void update(E item, int currIndex) throws NoSuchElementException{
        if (minHeapArray[currIndex].equals(item)) {
            minHeapArray[currIndex] = item;
            if (currIndex == 1) {
                bubbleDown(1);
                return;
            }
            if (minHeapArray[currIndex].compareTo(minHeapArray[currIndex/2]) < 0) {
                bubbleUp(currIndex);
                return;
            }
            bubbleDown(currIndex);
            return;
        }

        if (item.compareTo(minHeapArray[currIndex]) > 0) {
            if (minHeapArray[currIndex *2 +1] == null) {
                throw new NoSuchElementException();

            }
            update(item, currIndex*2 +1);
            return;
        }
        else {
            if (minHeapArray[currIndex *2] == null) {
                throw new NoSuchElementException();

            }
            update(item,currIndex *2);
            return;
        }

    }






}
