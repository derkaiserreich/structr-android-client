/**
 * Copyright (C) 2012-2015 Morgner UG (haftungsbeschränkt)
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.android.restclient;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.List;

/**
 * An abstract base class for REST entities on a structr server. This class encapsulates everything
 * neccesary to interact with a structr REST server in a single, convenient interface. Derive your
 * entities from this class, and you can easily create, load, update and delete them on a structr
 * server.
 *
 * @author Christian Morgner
 */
public abstract class StructrObject implements Serializable {

	private static final Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().setDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").create();

	private static HttpURLConnection conn = null;

	@Expose
	private String id = null;

	/**
	 * Override this method to load additional resources after
	 * the entity has been created from the JSON source. You can
	 * for example use this method to load nested fields of an
	 * entity synchronously.
	 */
	public void onDbLoad() {
	}

	/**
	 * @return the database ID of this entity
	 */
	public String getId() {
		return id;
	}

	/**
	 * Sets the ID of this entity. This method can be used to create an entity
	 * with pre-set ID.
	 * @param id the id of this entity
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * @return whether the REST server knows about the existance of this entity.
	 */
	public boolean isPersistent() {
		return id != null;
	}

	/**
	 * Creates this entity on the REST server. After successful creation, the ID
	 * of this entity will be set.
	 *
	 * @throws Throwable
	 */
	public void dbCreate() throws Throwable {
		create(buildPath("/", getEntityName()), this, getClass());
	}

	/**
	 * Stores the exposed attributes of this entity on the REST server.
	 *
	 * @throws Throwable
	 */
	public void dbStore() throws Throwable {
		store(buildPath("/", getEntityName(), "/", getId()), this, getClass());
	}

	/**
	 * Deletes this entity from the REST server. After successful deletion, the ID
	 * if this entity is null.
	 *
	 * @throws Throwable
	 */
	public void dbDelete() throws Throwable {
		delete(buildPath("/", getEntityName(), "/", getId()));
	}

	/**
	 * Loads an entity with the given type and ID from the REST server.
	 *
	 * @param type the type of the entity to load
	 * @param id the ID of the entity to load
	 * @return the entity from the REST server, or null if the entity was not found
	 * @throws Throwable
	 */
	public static <T extends StructrObject> T dbGet(final Class<T> type, final String id) throws Throwable {

		StructrObject newInstance = newInstance(type);
		if (newInstance != null) {
			return load(type, buildPath("/", newInstance.getEntityName(), "/", id));
		}

		return null;
	}

	/**
	 * Loads an entity with the given property value from the REST server.
	 *
	 * @param type the type of the entity to load
	 * @param key the property key
	 * @param value the property value
	 * @return the entity from the REST server, or null if the entity was not found
	 * @throws Throwable
	 */
	public static <T extends StructrObject> T dbLoad(final Class<T> type, final String key, final Object value) throws Throwable {

		StructrObject newInstance = newInstance(type);
		if (newInstance != null) {
			return load(type, buildPath("/", newInstance.getEntityName(), "?", key, "=", value));
		}

		return null;
	}

	/**
	 * Loads an entity from the given path. Use this method to load entities from
	 * arbitraty paths.
	 *
	 * @param type the type of the entity to load
	 * @param path the path of the entity to load
	 * @return the entity from the REST server, or null if the entity was not found
	 * @throws Throwable
	 */
	public static <T extends StructrObject> T dbLoad(final Class<T> type, final String path) throws Throwable {

		StructrObject newInstance = newInstance(type);
		if (newInstance != null) {
			return load(type, buildPath(path));
		}

		return null;
	}

	/**
	 * Fetches a sorted list of entities with the given type from the REST server.
	 *
	 * @param type the type of the entities to load
	 * @param sortKey the sort key
	 * @param asc whether to sort ascending or descending
	 * @param params additional parameters, may be empty
	 * @return a sorted list of entities matching the given type and parameters
	 * @throws Throwable
	 */
	public static <T extends StructrObject> List<T> dbList(final Class<T> type, final String sortKey, final boolean asc, final Object... params) throws Throwable {

		StructrObject newInstance = newInstance(type);
		if (newInstance != null) {
			return list(type, buildPath("/", newInstance.getEntityName(), "?sort=", sortKey, asc ? "" : "&order=desc", params));
		}

		return null;
	}

	/**
	 * Fetches a list of entities from the given path. Use this method to fetch arbitrary collections.
	 *
	 * @param type the type of the entities to load
	 * @param path the path of the entities to load
	 * @return a list of entities from the given path
	 * @throws Throwable
	 */
	public static <T extends StructrObject> List<T> dbList(final Class<T> type, final String path) throws Throwable {

		StructrObject newInstance = newInstance(type);
		if (newInstance != null) {
			return list(type, buildPath(path));
		}

		return null;
	}

	/**
	 * Fetches a sorted list of entities with the given type and property value from the REST server.
	 *
	 * @param type the type of the entities to load
	 * @param key the property key to search for
	 * @param value the property value to search for
	 * @param sortKey the sort key
	 * @param asc whether to sort ascending or descending
	 * @return a sorted list of entities matching the given type and property value
	 * @throws Throwable
	 */
	public static <T extends StructrObject> List<T> dbFind(final Class<T> type, final String key, final Object value, final String sortKey, final boolean asc) throws Throwable {

		StructrObject newInstance = newInstance(type);
		if (newInstance != null) {
			return list(type, buildPath("/", newInstance.getEntityName(), "?", key, "=", value, "&sort=", sortKey, asc ? "" : "&order=desc"));
		}

		return null;
	}

	/**
	 * Fetches a sorted list of child entities for a given parent from the REST server.
	 *
	 * @param type the parent's type
	 * @param id the parent's ID
	 * @param childType the children's type
	 * @param sortKey the sort key
	 * @param asc whether to sort ascending or descending
	 * @return a sorted list of entities that are children of a given parent entity with the given ID
	 * @throws Throwable
	 */
	public static <T extends StructrObject> List<T> dbFind(final Class type, final String id, final Class<T> childType, final String sortKey, final boolean asc) throws Throwable {

		StructrObject childInstance = newInstance(childType);
		StructrObject newInstance = newInstance(type);
		if (newInstance != null) {
			return list(childType, buildPath("/", newInstance.getEntityName(), "/", id, "/", childInstance.getEntityName(), "?sort=", sortKey, asc ? "" : "&order=desc"));
		}

		return null;
	}

	/**
	 * Fetches a single child entitiy with a given ID from a parent with a given ID. This method can
	 * be used to check for an existing relationship between the to entities. If a valid path exists
	 * from one entity to the other, the entities are related.
	 *
	 * @param type the parent's type
	 * @param id the parent's ID
	 * @param childType the child's type
	 * @param childId the child's ID
	 * @param sortKey the sort key
	 * @param asc whether to sort ascending or descending
	 * @return the child entity with the given ID, if there is a relationship with the parent
	 * @throws Throwable
	 */
	public static <T extends StructrObject> T dbFind(final Class type, final String id, final Class<T> childType, final String childId, final String sortKey, final boolean asc) throws Throwable {

		StructrObject childInstance = newInstance(childType);
		StructrObject newInstance = newInstance(type);
		if (newInstance != null) {
			return load(childType, buildPath("/", newInstance.getEntityName(), "/", id, "/", childInstance.getEntityName(), "/", childId));
		}

		return null;
	}



	// ----- private methods -----
	private String getEntityName() {
		return getClass().getSimpleName().toLowerCase();
	}


	// ----- private static methods -----
	private static <T extends StructrObject> T load(final Class<T> type, final String path) throws Throwable {

        T result                           = null;
        Throwable throwable                = null;

        try {
            conn = (HttpURLConnection) new URL(path).openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("X-USer", StructrConnector.getUserName());
            conn.setRequestProperty("X-Password", StructrConnector.getPassword());

            String response = "";

			if (conn.getResponseCode() == 200) {

                InputStreamReader isr = new InputStreamReader(conn.getInputStream());
                BufferedReader br = new BufferedReader(isr);
                String line = "";
                while((line = br.readLine()) != null){
                    response+=line;
                }

				StructrEntityResult<T> entityResult = (StructrEntityResult<T>)gson.fromJson(response, getEntityTypeToken(type));
				if (entityResult != null) {

					result = entityResult.getResult();
					result.onDbLoad();
				}

			} else {

				throw new StructrException(conn.getResponseCode(), conn.getResponseMessage(), response);
			}

		} catch(Throwable t) {

			throwable = t;

		} finally {
            if(conn != null)
                conn.disconnect();
		}

		if (throwable != null) {
			throw throwable;
		}

		return result;
	}

	private static int create(String path, StructrObject entity, Type type) throws Throwable {

		/*final AndroidHttpClient httpClient = getHttpClient();
		final HttpPost httpPost            = new HttpPost(path);
		HttpResponse response              = null;*/
		Throwable throwable                 = null;
		String response                     = "";
        int responseCode                    = 0;

		try {
            //Cast the entity to Json
			StringBuilder buf = new StringBuilder();
			gson.toJson(entity, type, buf);

            //Configure the Connection
            conn = (HttpURLConnection) new URL(path).openConnection();
            conn.setRequestMethod("POST");
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("X-User", StructrConnector.getUserName());
            conn.setRequestProperty("X-Password", StructrConnector.getPassword());

            //Write Json
            OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
            wr.write(buf.toString());
            wr.flush();

           	responseCode = conn.getResponseCode();
			if (responseCode == 201) {

                //Read the response message
                InputStreamReader isr = new InputStreamReader(conn.getInputStream());
                BufferedReader br = new BufferedReader(isr);
                String line = "";
                while((line = br.readLine()) != null){
                    response+=line;
                }

				String location = conn.getHeaderField("Location");
				String newId = getIdFromLocation(location);

				// only set ID of it's not already set
				if (entity.getId() == null) {
					entity.setId(newId);
				}

			} else {

				throw new StructrException(conn.getResponseCode(),conn.getResponseMessage(), response);
			}

		} catch(Throwable t) {
			throwable = t;
		} finally {
            if(conn != null)
                conn.disconnect();
		}

		if (throwable != null) {
			throw throwable;
		}

		return responseCode;
	}

	private static int store(String path, StructrObject entity, Type type) throws Throwable {

		Throwable throwable                 = null;
        String response                     = "";
		int responseCode                    = 0;

		try {
			StringBuilder buf = new StringBuilder();
			gson.toJson(entity, type, buf);

            //Configure the Connection
            conn = (HttpURLConnection) new URL(path).openConnection();
            conn.setRequestMethod("POST");
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("X-User", StructrConnector.getUserName());
            conn.setRequestProperty("X-Password", StructrConnector.getPassword());

            //Write Json
            OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
            wr.write(buf.toString());
            wr.flush();

            //Read the response message
            InputStreamReader isr = new InputStreamReader(conn.getInputStream());
            BufferedReader br = new BufferedReader(isr);
            String line = "";
            while((line = br.readLine()) != null){
                response+=line;
            }

			responseCode = conn.getResponseCode();

		} catch(Throwable t) {
			throwable = t;

		} finally {
            if(conn != null)
                conn.disconnect();
		}

		if (throwable != null) {
			throw throwable;
		}

		return responseCode;
	}

	private static int delete(String path) throws Throwable {

		String response                  = "";
		Throwable throwable              = null;
		int responseCode                 = 0;

		try {
            conn = (HttpURLConnection) new URL(path).openConnection();
            conn.setRequestMethod("DELETE");
            conn.setRequestProperty("X-User", StructrConnector.getUserName());
            conn.setRequestProperty("X-Passoword", StructrConnector.getPassword());

            //Read the response message
            InputStreamReader isr = new InputStreamReader(conn.getInputStream());
            BufferedReader br = new BufferedReader(isr);
            String line = "";
            while((line = br.readLine()) != null){
                response+=line;
            }

			responseCode = conn.getResponseCode();

		} catch(Throwable t) {

			throwable = t;

		} finally {
            if(conn != null)
                conn.disconnect();
		}

		if (throwable != null) {
			throw throwable;
		}

		return responseCode;
	}

	private static <T extends StructrObject> List<T> list(final Class<T> type, final String path) throws Throwable {

        String response         = "";
		List<T> result          = null;
        Throwable throwable     = null;

		try {
            conn = (HttpURLConnection) new URL(path).openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("X-User", StructrConnector.getUserName());
            conn.setRequestProperty("X-Password", StructrConnector.getPassword());
            conn.setRequestProperty("Accept-Charset", "UTF-8");

            //Read the response message
            InputStreamReader isr = new InputStreamReader(conn.getInputStream());
            BufferedReader br = new BufferedReader(isr);
            String line = "";
            while((line = br.readLine()) != null){
                response+=line;
            }

			if (conn.getResponseCode() == 200) {

				StructrCollectionResult<T> collectionResult = (StructrCollectionResult<T>) gson.fromJson(response, getCollectionTypeToken(type));
				if (collectionResult != null) {

					result = collectionResult.getResult();
					for(StructrObject obj : result) {

						obj.onDbLoad();
					}

				} else {

					result = Collections.emptyList();
				}

			} else {

				throw new StructrException(conn.getResponseCode(), conn.getResponseMessage(), response);
			}

		} catch(Throwable t) {

            throwable = t;

		} finally {
            if(conn != null)
                conn.disconnect();
		}

		if (throwable != null) {
			throw throwable;
		}

		return result;
	}

	private static String buildPath(String url, Object... params) {

		StringBuilder path = new StringBuilder();
		String base = StructrConnector.getServer();

		path.append(base);
		if (!base.endsWith("/")) {
			path.append("/rest/");
		}
		else
        path.append("rest/");
		path.append(url);

		for(Object o : params) {

			if (o.getClass().isArray()) {
				Object[] array = (Object[])o;
				for(Object a : array) {
					path.append(a);
				}

			} else {
				path.append(o);
			}
		}

		return path.toString();
	}


	private static String getIdFromLocation(String location) {
		int pos = location.lastIndexOf("/");
		return location.substring(pos+1);
	}

	private static <T extends StructrObject> T newInstance(final Class<T> type) {
		try { return type.newInstance(); } catch(Throwable t) {}
		return null;
	}

	private static Type getCollectionTypeToken(Class type) {
		return new ParameterizedTypeImpl(StructrCollectionResult.class, type);
	}

	private static Type getEntityTypeToken(Class type) {
		return new ParameterizedTypeImpl(StructrEntityResult.class, type);
	}

	// ----- private static nested classes -----
	private static class ParameterizedTypeImpl implements ParameterizedType {

		private Class genericType = null;
		private Class rawType = null;

		public ParameterizedTypeImpl(Class rawType, Class genericType) {
			this.rawType = rawType;
			this.genericType = genericType;
		}

		@Override
		public Type[] getActualTypeArguments() {
			return new Type[] { genericType };
		}

		@Override
		public Type getOwnerType() {
			return StructrObject.class;
		}

		@Override
		public Type getRawType() {
			return rawType;
		}

		@Override
		public boolean equals(Object o) {
			return o.hashCode() == this.hashCode();
		}

		@Override
		public int hashCode() {
			return (genericType.hashCode() * 31) + rawType.hashCode();
		}
	}

	private static class StructrEntityResult<T extends StructrObject> {

		@Expose
		T result = null;

		public void setResult(T result) {
			this.result = result;
		}

		public T getResult() {
			return result;
		}

	}

	private class StructrCollectionResult<T extends StructrObject> {
		@Expose
		List<T> result = null;

		public void setResult(List<T> result) {
			this.result = result;
		}

		public List<T> getResult() {
			return result;
		}
	}
}
