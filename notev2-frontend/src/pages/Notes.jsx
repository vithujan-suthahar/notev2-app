import { useEffect, useState } from "react";
import { getNotes, createNote, deleteNote, updateNote } from "../api/notes";
import { getCurrentUser } from "../api/auth";
import { useNavigate } from "react-router-dom";
import { isAdmin } from "../utils/auth";
import API from "../api/api";

function extractErrorMessage(errorData) {
  if (!errorData) return "Ein Fehler ist aufgetreten";

  // Fall 1: Validation-Errors (@Valid) -> { message: "...", errors: { title: "...", content: "..." } }
  if (errorData.errors) {
    return Object.values(errorData.errors).join(", ");
  }

  // Fall 2: Andere Backend-Errors (AuthResponse-Style) -> { message: "..." }
  if (errorData.message) {
    return errorData.message;
  }

  return "Ein Fehler ist aufgetreten";
}

function Notes() {
  const [notes, setNotes] = useState([]);
  const [selectedNote, setSelectedNote] = useState(null);

  const [title, setTitle] = useState("");
  const [content, setContent] = useState("");

  const [username, setUsername] = useState("");
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const navigate = useNavigate();

  useEffect(() => {
    fetchNotes();
    fetchUser();
  }, []);

  const fetchNotes = async () => {
    try {
      const response = await getNotes();
      setNotes(response.data);
    } catch (error) {
      setError(extractErrorMessage(error.response?.data));
    }
  };

  const fetchUser = async () => {
    try {
      const response = await getCurrentUser();
      setUsername(response.data.message);
    } catch (error) {
      setError(extractErrorMessage(error.response?.data));
    }
  };

  const handleSelect = (note) => {
    if (selectedNote && selectedNote.id === note.id) {
      setSelectedNote(null);
      setTitle("");
      setContent("");
      return;
    } else {
      setSelectedNote(note);
      setTitle(note.title);
      setContent(note.content);
    }
  };

  const handleCreateNote = async () => {
    try {
      const response = await createNote({ title: "Neue Notiz", content: "" });
      const newNote = response.data;

      setNotes((prevNotes) => [newNote, ...prevNotes]);

      setSelectedNote(newNote);
      setTitle(newNote.title);
      setContent(newNote.content);
      setError("");
    } catch (error) {
      setError(extractErrorMessage(error.response?.data));
      setSuccess("");
    }
  };

  const handleUpdate = async () => {
    try {
      // Nur title/content senden - Backend-DTO nimmt eh nur diese Felder entgegen
      const response = await updateNote(selectedNote.id, { title, content });
      const updated = response.data;

      setNotes((prevNotes) =>
        prevNotes.map((note) => (note.id === updated.id ? updated : note))
      );
      setSelectedNote(updated);

      setSuccess("Aktualisiert");
      setTimeout(() => {
        setSuccess("");
      }, 2000);
      setError("");
    } catch (error) {
      setError(extractErrorMessage(error.response?.data));
    }
  };

  const handleDelete = async (id) => {
    try {
      await deleteNote(id);

      setNotes((prevNotes) => prevNotes.filter((note) => note.id !== id));

      setSuccess("Notiz gelöscht");
      setTimeout(() => {
        setSuccess("");
      }, 2000);
      setError("");
      setTitle("");
      setContent("");
      setSelectedNote(null);
    } catch (error) {
      setError(extractErrorMessage(error.response?.data));
      setSuccess("");
    }
  };

  const handleLogout = async () => {
    try {
      await API.post("/auth/logout");
    } catch (error) {
    } finally {
      localStorage.removeItem("token");
      window.location.href = "/login";
    }
  };

  const handleAdmin = () => {
    if (!isAdmin()) {
      setError("Zugriff verweigert: Admins only");
      return;
    }
    navigate("/user");
  };

  return (
    <div className="p-6 bg-gray-100 min-h-screen">
      <div className="flex justify-between mb-4">
        <h2 className="text-2xl font-bold">Meine Notizen</h2>
        {error && <p className="text-red-500 mb-2">{error}</p>}
        {success && <p className="text-green-500 mb-2">{success}</p>}

        <button
          onClick={handleLogout}
          className="bg-red-500 text-white px-4 py-1 rounded hover:bg-red-600"
        >
          Logout
        </button>
      </div>
      {isAdmin() && (
        <button
          onClick={handleAdmin}
          className="bg-purple-500 text-white px-4 py-2 rounded hover:bg-purple-600"
        >
          Benutzerverwaltung
        </button>
      )}

      <p className="mb-4">Eingeloggt als: {username}</p>

      <div className="flex gap-2 mb-6">
        <button
          onClick={handleCreateNote}
          className="bg-green-500 text-white px-4 py-2 rounded hover:bg-green-600"
        >
          Neue Notiz erstellen
        </button>
      </div>

      <div className="flex flex-1 gap-6 overflow-hidden">
        <ul className="w-1/3 space-y-2">
          {notes.length === 0 ? (
            <li className="text-gray-400 italic p-4">
              Noch keine Notizen vorhanden.
            </li>
          ) : (
            notes.map((note) => (
              <li
                key={note.id}
                onClick={() => handleSelect(note)}
                className={`p-4 rounded shadow cursor-pointer transition-colors ${selectedNote?.id === note.id
                    ? "bg-blue-500 text-white"
                    : "bg-white hover:bg-gray-50"
                  }`}
              >
                <div className="flex justify-between items-center">
                  <span className="font-semibold truncate">{note.title}</span>
                  <button
                    onClick={(e) => {
                      e.stopPropagation();
                      handleDelete(note.id);
                    }}
                    className="bg-red-500 text-white px-4 py-1 rounded hover:bg-red-600"
                  >
                    Löschen
                  </button>
                </div>
              </li>
            ))
          )}
        </ul>

        <div className="w-2/3 bg-white p-8 rounded shadow-lg border border-gray-200 min-h-[300px]">
          {selectedNote ? (
            <>
              <input
                type="text"
                placeholder="Titel..."
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                className="w-full text-2xl font-bold mb-4 border-b p-2"
              />
              <textarea
                className="w-full h-60 border p-2 rounded"
                value={content}
                onChange={(e) => setContent(e.target.value)}
              />
              <button
                onClick={handleUpdate}
                className="mt-4 bg-blue-500 text-white px-4 py-2 rounded hover:bg-blue-600"
              >
                Speichern
              </button>
            </>
          ) : (
            <div className="h-full flex items-center justify-center text-gray-400 italic">
              Wähle eine Notiz aus der Liste aus, um den Inhalt zu lesen.
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

export default Notes;
