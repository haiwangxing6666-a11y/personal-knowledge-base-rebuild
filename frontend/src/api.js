export async function request(url, options) {
    const response = await fetch(url, options);
    if (response.status === 204) return;

    const body = await response.json();
    if (!response.ok) throw new Error(body.message);
    return body;
}

export function jsonRequest(method, body) {
    return {
        method,
        headers: {"Content-Type": "application/json"},
        body: JSON.stringify(body)
    };
}
