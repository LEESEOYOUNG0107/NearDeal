import { useEffect, useState, type FormEvent } from 'react'

const API_BASE = 'http://localhost:8080'

function App() {
  const [title, setTitle] = useState('')
  const [price, setPrice] = useState('')
  const [category, setCategory] = useState('DIGITAL')
  const [description, setDescription] = useState('')
  const [region, setRegion] = useState('')
  const [result, setResult] = useState('')
  const [products, setProducts] = useState<unknown[]>([])

  const loadProducts = async () => {
    const res = await fetch(`${API_BASE}/api/products`)
    setProducts(await res.json())
  }

  useEffect(() => {
    loadProducts()
  }, [])

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault()
    try {
      const res = await fetch(`${API_BASE}/api/products`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ title, price: Number(price), category, description, region }),
      })
      const body = await res.text()
      setResult(`status: ${res.status}\n${body}`)
      if (res.ok) loadProducts()
    } catch (err) {
      setResult(String(err))
    }
  }

  return (
    <div>
      <h1>상품 등록</h1>
      <form onSubmit={handleSubmit}>
        <div>
          상품명: <input value={title} onChange={(e) => setTitle(e.target.value)} />
        </div>
        <div>
          가격: <input value={price} onChange={(e) => setPrice(e.target.value)} />
        </div>
        <div>
          카테고리: <input value={category} onChange={(e) => setCategory(e.target.value)} />
        </div>
        <div>
          설명: <input value={description} onChange={(e) => setDescription(e.target.value)} />
        </div>
        <div>
          지역: <input value={region} onChange={(e) => setRegion(e.target.value)} />
        </div>
        <button type="submit">등록</button>
      </form>
      <pre>{result}</pre>

      <h1>상품 목록</h1>
      <pre>{JSON.stringify(products, null, 2)}</pre>
    </div>
  )
}

export default App
