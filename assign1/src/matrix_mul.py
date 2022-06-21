import time
def OnMult(m_ar, m_br):

    pha = []
    phb = []
    phc = []
    for i in range(m_ar):
        for j in range(m_ar):
            pha.insert((i*m_ar + j), 1.0) 
            phb.insert((i*m_br + j), i+1)
            phc.insert((i*m_br + j), 0)
    
    start_time = time.time()

    for i in range (m_ar):
        for j in range (m_br):
            temp = 0
            for k in range(m_ar):
                temp += pha[i*m_ar+k] * phb[k*m_br+j]
            phc[i*m_ar+j] = temp

    end_time = time.time()

    print("Result matrix: ")
    for i in range (1):
        for j in range (min(10,m_br)):
            print(phc[j], " ")
    print("Time: ", (end_time - start_time))

def OnMultLine(m_ar, m_br):


    pha = []
    phb = []
    phc = []
    for i in range(m_ar):
        for j in range(m_ar):
            pha.insert((i*m_ar + j), 1.0) 
            phb.insert((i*m_br + j), i+1)
            phc.insert((i*m_br + j), 0)
    
    start_time = time.time()

    for i in range (m_ar):
        for k in range(m_ar):
            for j in range (m_br):
                phc[i*m_ar+j]+=pha[i*m_ar+k] * phb[k*m_br+j]
    
    end_time = time.time()

    print("Result matrix: ")
    for i in range (1):
        for j in range (min(10,m_br)):
            print(phc[j], " ")
    print("Time: ", (end_time - start_time))

    

op = input("1. Multiplication \n2. Line Multiplication \n")
lin = input("Dimensions: lins=cols? ")

if op == "1":    
    OnMult(int(lin), int(lin))
elif op == "2":
    OnMultLine(int(lin), int(lin))
