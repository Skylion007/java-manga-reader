package org.skylion.mangareader.util;

import java.io.Serializable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import javax.swing.AbstractListModel;
import javax.swing.MutableComboBoxModel;

/**
 * A model design to automatically update based of a the key values of a Hashmap
 * @author Skylion
 *
 * @param <K> The generic representing key of the Hashmap
 * @param <V> The generic representing the value of the Hashmap
 */
public class HashMapComboBoxModel<K, V> extends AbstractListModel<K> implements MutableComboBoxModel<K>, 
Serializable {
	
	/**
	 * Auto-generated
	 */
	private static final long serialVersionUID = -1293826656458176439L;

	/**
	 * The Selected Item
	 */
	private K selectedItem;
	
	private LinkedHashMap<K,V> data;
	
	private int prevSize;
	
	public HashMapComboBoxModel(LinkedHashMap<K,V> data){
		this.data = data;
		prevSize = data.size();
	}
	
	@Override
	public K getElementAt(int index) {
		List<Entry<K,V>> randAccess = new ArrayList<Entry<K,V>>((Collection<? extends Entry<K, V>>)data.entrySet());
        return randAccess.get(index).getKey();
	}

	@Override
	public int getSize() {
		int size = data.size();
		if(size != prevSize){
			contentsUpdated();//Hacky way to make sure the List is up to date.
			prevSize = size;
		}
		return size;
	}

	@Override
	public K getSelectedItem() {
		return selectedItem;
	}

	@SuppressWarnings("unchecked")//Equals() implements the check
	@Override
	public void setSelectedItem(Object anItem) {
		for(K keys: data.keySet()){
			if(keys.equals(anItem)){
				this.selectedItem = (K) anItem;
				return;
			}
		}
	}

	@Override
	public void addElement(Object obj) {
		addElement(downcastToEntry(obj));
	}

	@SuppressWarnings("unchecked")
	private Entry<K,V> downcastToEntry(Object obj){
		if(obj instanceof Entry && obj.getClass().isAssignableFrom(
				data.entrySet().iterator().next().getClass())){
			return (Entry<K,V>)obj;
		}
		return null;
	}
		
	/**
	 * Adds an Entry value to the hashmap
	 * @param obj The Entry value you want to add
	 * @return return true if added false otherwise
	 */
	public boolean addElement(Entry<K,V> obj){
		if(obj == null){return false;}
		return this.data.entrySet().add(obj);
	}
	
	
	@Override
	public void insertElementAt(Object obj, int index) {
		Entry<K,V> entry = downcastToEntry(obj);
		addToMapAtIndex(index, entry.getKey(), entry.getValue());
	}

	private void addToMapAtIndex(int index, K key, V value) {
		assert (data != null);
		assert !data.containsKey(key);
		assert (index >= 0) && (index < data.size());

		int i = 0;
		List<Entry<K, V>> rest = new ArrayList<Entry<K, V>>();
		for (Entry<K, V> entry : data.entrySet()) {
			if (i++ >= index) {
				rest.add(entry);
			}
		}
		data.put(key, value);
		for (int j = 0; j < rest.size(); j++) {
			Entry<K, V> entry = rest.get(j);
			data.remove(entry.getKey());
			data.put(entry.getKey(), entry.getValue());
		}
	}

	@Override
	public void removeElement(Object obj) {
		data.remove(obj);
	}
	
	@Override
	public void removeElementAt(int index) {
		data.remove(getElementAt(index));
	}

	private void contentsUpdated(){
		super.fireContentsChanged(this, 0, data.size());
	}
}
