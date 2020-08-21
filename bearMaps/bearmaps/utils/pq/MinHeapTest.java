package bearmaps.utils.pq;

import org.junit.Test;
import static org.junit.Assert.*;


public class MinHeapTest {

    @Test
    public void minTest1() {
        MinHeap<Integer> a = new MinHeap<>();
        a.insert(6);
        a.insert(8);
        a.insert(4);
        a.insert(2);
        a.insert(7);
        assert(a.findMin() == 2);
    }


    @Test
    public void minTest2() {
        MinHeap<Integer> a = new MinHeap<>();
        a.insert(6);
        a.insert(8);
        a.insert(4);
        a.insert(2);
        a.insert(7);
        a.removeMin();
        assert(a.findMin() == 4);
    }

    @Test
    public void minTest3() {
        MinHeap<Integer> a = new MinHeap<>();
        a.insert(6);
        a.insert(8);
        a.insert(4);
        a.insert(2);
        a.insert(7);
        a.removeMin();
        a.removeMin();
        a.removeMin();
        assert(a.findMin() == 7);
        assertFalse(a.contains(6));
    }

    @Test
    public void insertTest1() {
        MinHeap<Integer> a = new MinHeap<>();
        a.insert(6);
        a.insert(8);
        a.insert(4);
        a.insert(2);
        a.insert(7);
        a.insert(1);
        assert(a.findMin() == 1);
    }


    @Test
    public void insertTest3() {
        MinHeap<Integer> a = new MinHeap<>();
        a.insert(8);
        a.insert(6);
        a.insert(7);
        assert(a.findMin() == 6);
    }


    @Test
    public void updateInAcsendingOrder() {
        PriorityQueue<String> a = new MinHeapPQ<>();
        a.insert("hello", 3);
        a.insert("yoyoyo", 2);
        a.insert("my name is", 1);
        a.changePriority("my name is", 1);
        a.changePriority("yoyoyo", 2);
        a.changePriority("hello", 3);
        assert (a.peek().equals("my name is"));
    }

}
