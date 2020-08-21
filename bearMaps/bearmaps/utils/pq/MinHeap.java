package bearmaps.utils.pq;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.NoSuchElementException;

/* A MinHeap class of Comparable elements backed by an ArrayList. */
public class MinHeap<E extends Comparable<E>> {

    /* An ArrayList that stores the elements in this MinHeap. */
    private ArrayList<E> contents;
    private int size = 0;
    private HashMap<E, Integer> indexes = new HashMap<>();
    // implementing the more optimized version)

    /* Initializes an empty MinHeap. */
    public MinHeap() {
        contents = new ArrayList<>();
        contents.add(null);
    }

    /* Returns the element at index INDEX, and null if it is out of bounds. */
    private E getElement(int index) {
        if (index >= contents.size()) {
            return null;
        } else {
            return contents.get(index);
        }
    }

    /* Sets the element at index INDEX to ELEMENT. If the ArrayList is not big
       enough, add elements until it is the right size. */
    private void setElement(int index, E element) {
        while (index >= contents.size()) {
            contents.add(null);
        }
        contents.set(index, element);
    }

    /* Swaps the elements at the two indices. */
    private void swap(int index1, int index2) {
        E element1 = getElement(index1);
        E element2 = getElement(index2);
        setElement(index2, element1);
        setElement(index1, element2);

        indexes.put(element1, index2);
        indexes.put(element2, index1);
    }

    /* Prints out the underlying heap sideways. Use for debugging. */
    @Override
    public String toString() {
        return toStringHelper(1, "");
    }

    /* Recursive helper method for toString. */
    private String toStringHelper(int index, String soFar) {
        if (getElement(index) == null) {
            return "";
        } else {
            String toReturn = "";
            int rightChild = getRightOf(index);
            toReturn += toStringHelper(rightChild, "        " + soFar);
            if (getElement(rightChild) != null) {
                toReturn += soFar + "    /";
            }
            toReturn += "\n" + soFar + getElement(index) + "\n";
            int leftChild = getLeftOf(index);
            if (getElement(leftChild) != null) {
                toReturn += soFar + "    \\";
            }
            toReturn += toStringHelper(leftChild, "        " + soFar);
            return toReturn;
        }
    }

    /* Returns the index of the left child of the element at index INDEX. */
    private int getLeftOf(int index) {
        return index *2;
    }

    /* Returns the index of the right child of the element at index INDEX. */
    private int getRightOf(int index) {
        return index*2 +1;
    }
//
//    /* Returns the index of the parent of the element at index INDEX. */
//    private int getParentOf(int index) {
//        return -1;
//    }
//
//    /* Returns the index of the smaller element. At least one index has a
//       non-null element. If the elements are equal, return either index. */
//    private int min(int index1, int index2) {
//        return -1;
//    }

    /* Returns but does not remove the smallest element in the MinHeap. */
    public E findMin() {
        if (contents.get(1) == null) {
            throw new NoSuchElementException();
        }
        return getElement(1);
    }

    /* Bubbles up the element currently at index INDEX. */
    private void bubbleUp(int index) {
        if (index == 1) {
            return;
        }
        if (getElement(index/2).compareTo(getElement(index)) > 0) {
            swap(index, index/2);
            bubbleUp(index/2);
        }
    }

    /* Bubbles down the element currently at index INDEX. */
    private void bubbleDown(int index) {

        if (index == size) {
            return;
        }
        if (getElement(index) ==null) {
            return;
        }

        boolean leftExists = (!(getElement(index*2) == null));
        boolean rightExists = (!(getElement(index*2 +1) == null));

        if (leftExists && rightExists) {
            boolean leftIsSmaller = (getElement(index*2).compareTo(getElement(index)) < 0);
            boolean rightIsSmaller = (getElement(index*2 +1).compareTo(getElement(index)) < 0);
            boolean leftIsSmallerThanRight = (getElement(index*2).compareTo(getElement(index *2 +1)) < 0);

            if (leftIsSmaller && rightIsSmaller) {

                if (leftIsSmallerThanRight) {
                    swap(index, index*2);
                    bubbleDown(index*2);
                } else {
                    swap(index, index*2 +1);
                    bubbleDown(index*2 +1);
                }
                return;
            }

            if (rightIsSmaller) {
                swap(index, index*2 +1);
                bubbleDown(index*2 +1);
                return;
            }

            if (leftIsSmaller) {
                swap(index, index*2);
                bubbleDown(index*2);
                return;
            }
        }

        if (leftExists & !(rightExists)) {
            boolean leftIsSmaller = (getElement(index*2).compareTo(getElement(index)) < 0);
            if (leftIsSmaller) {
                swap(index, index*2);
                bubbleDown(index*2);
                return;
            }
        }

        if (rightExists & !(leftExists)) {
            boolean rightIsSmaller = (getElement(index*2 +1).compareTo(getElement(index)) < 0);
            if (rightIsSmaller){
                swap(index, index*2 +1);
                bubbleDown(index*2 +1);
            }
        }
    }

    /* Returns the number of elements in the MinHeap. */
    public int size() {
        return size;
    }

    /* Inserts ELEMENT into the MinHeap. If ELEMENT is already in the MinHeap,
       throw an IllegalArgumentException.*/
    public void insert(E element) throws IllegalArgumentException{
        if (contains(element)) {
            throw new IllegalArgumentException();
        }
        setElement(size + 1, element);
        indexes.put(element, size +1);
        size++;
        bubbleUp(size);
    }

    /* Returns and removes the smallest element in the MinHeap. */
    public E removeMin() {
        if (size == 0) {
            throw new NoSuchElementException();
        }
        E toReturn = getElement(1);
        indexes.remove(toReturn);
        setElement(1, getElement(size));
        indexes.put(getElement(1), 1);
        contents.remove(size);
        size--;
        bubbleDown(1);
        return toReturn;
    }

    public int findIndex(E element) {
        return indexes.get(element);
    }

    /* Replaces and updates the position of ELEMENT inside the MinHeap, which
       may have been mutated since the initial insert. If a copy of ELEMENT does
       not exist in the MinHeap, throw a NoSuchElementException. Item equality
       should be checked using .equals(), not ==. */
    public void update(E element) throws NoSuchElementException{

        if (indexes.containsKey(element)) {
            int indexOfElement = findIndex(element);
            setElement(indexOfElement, element);
            if (indexOfElement == 1) {
                    bubbleDown(1);
                    return;
                }
            if (element.compareTo(getElement(indexOfElement/2)) < 0) {
                bubbleUp(indexOfElement);
                    return;
                }
                bubbleDown(indexOfElement);
                return;
        }

        throw new NoSuchElementException();
    }

    /* Returns true if ELEMENT is contained in the MinHeap. Item equality should
       be checked using .equals(), not ==. */
    public boolean contains(E element) {

        return indexes.containsKey(element);


    }
}
