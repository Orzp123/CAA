"use client";

import { useEffect, useState } from "react";
import { agentApi } from "@/lib/api";

interface Agent {
  id: string;
  name: string;
  description: string;
  provider: string;
  model: string;
  status: string;
}

export default function AgentsPage() {
  const [agents, setAgents] = useState<Agent[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    agentApi.list()
      .then((res) => setAgents(res.data))
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <div className="p-8 text-gray-400">Loading agents...</div>;
  if (error) return <div className="p-8 text-red-400">Error: {error}</div>;

  return (
    <div className="p-8">
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold">Agents</h1>
        <button className="px-4 py-2 bg-brand-500 hover:bg-brand-600 rounded-lg text-sm font-medium transition-colors">
          + New Agent
        </button>
      </div>
      {agents.length === 0 ? (
        <div className="text-center py-16 text-gray-500">
          No agents yet. Create your first agent to get started.
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {agents.map((agent) => (
            <div key={agent.id} className="p-5 bg-gray-900 border border-gray-800 rounded-xl">
              <div className="flex items-start justify-between mb-3">
                <h3 className="font-semibold">{agent.name}</h3>
                <span className={`text-xs px-2 py-1 rounded-full ${
                  agent.status === "ACTIVE" ? "bg-green-900 text-green-300" : "bg-gray-800 text-gray-400"
                }`}>{agent.status}</span>
              </div>
              <p className="text-sm text-gray-400 mb-3">{agent.description}</p>
              <div className="flex items-center gap-2 text-xs text-gray-500">
                <span className="bg-gray-800 px-2 py-1 rounded">{agent.provider}</span>
                <span className="bg-gray-800 px-2 py-1 rounded">{agent.model}</span>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
