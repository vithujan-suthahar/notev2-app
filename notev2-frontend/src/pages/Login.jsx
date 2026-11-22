import { useNavigate } from "react-router-dom";
import { useState } from "react";
import API from "../api/api";




function Login({ setToken }) {

  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const navigate = useNavigate();


  const handleLogin = async (e) => {
    e.preventDefault();
    setError("");
    setSuccess("");
    try {
      const response = await API.post("/auth/login", { username, password });
      setSuccess(response.data.message);
      localStorage.setItem("token", response.data.accessToken);
      setToken(response.data.accessToken);
      navigate("/notes");
    } catch (error) {
      const apiData = error.response?.data;

      if (apiData?.errors) {
        // Wandelt das errors-Objekt { username: "...", password: "..." } in einen Lesbaren Text um
        const detailMessages = Object.values(apiData.errors).join(" | ");
        setError(detailMessages);
      } else if (apiData?.message) {
        setError(apiData.message);
      } else {
        setError("Verbindung zum Server fehlgeschlagen.");
      }
    }
    setUsername("");
    setPassword("");
  }


  return (
    <div className="flex items-center justify-center h-screen bg-gray-100">
      <form
        onSubmit={handleLogin}
        className="bg-white p-8 rounded-2xl shadow-md w-80"
      >
        <h2 className="text-2xl font-bold mb-6 text-center">Login</h2>

        {error && <p className="text-red-500 mb-2">{error}</p>}
        {success && <p className="text-green-500 mb-2">{success}</p>}

        <input
          type="text"
          placeholder="Username"
          value={username}
          onChange={(e) => setUsername(e.target.value)}
          className="w-full mb-3 p-2 border rounded"
        />

        <input
          type="password"
          placeholder="Passwort"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          className="w-full mb-4 p-2 border rounded"
        />
        <div className="flex gap-4 mt-2">
          <button
            type="submit"
            className="w-full bg-blue-500 text-white p-2 rounded hover:bg-blue-600"
          > Login
          </button>
          <button
            type="button"
            onClick={() => navigate("/register")}
            className="w-full bg-gray-500 text-white p-2 rounded hover:bg-gray-600"
          > Registrieren
          </button>
        </div>
      </form>
    </div>
  );
}
export default Login;